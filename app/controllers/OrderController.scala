package controllers

import java.text.SimpleDateFormat
import java.util.Date

import javax.inject._
import play.api.Logger
import play.api.http.HttpEntity
import play.api.data.Forms._
import play.api.data.Form
import play.api.mvc._
import cats.effect.IO
import akka.util.ByteString
import services.SimbaAlias._
import models.{OrderForDisplay, OrderInput, OrderModel, PassingDate}
import services.SimbaHTMLHelper.{getLastSundayFromGivenDate, getNextSundayFromGivenDate}

@Singleton
class OrderController @Inject()(orderModel: OrderModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
  private val orderForm = Form(
    mapping(
      "id" -> ignored(None: Option[Int]),
      "customerId" -> number,
      "addressId" -> number,
      "invadedFromCustomer" -> optional(number),
      "orderDay" -> date, "deliveryDay" -> date,
      "deliverFrom" -> nonEmptyText.verifying(_.contains(':')), "deliverTo" -> nonEmptyText.verifying(_.contains(':')),
      "inOrder" -> list(number),
      "total" -> number,
      "discount" -> optional(number),
      "payment" -> nonEmptyText,
      "offlineDelivery" -> boolean, "deliveryOnMonday" -> boolean,
      "paid" -> boolean, "delivered" -> boolean,
      "note" -> optional(text)
    )(OrderInput.apply)(OrderInput.unapply))
  private val passDateForm = Form(
    mapping(
      "date" -> date
    )(PassingDate.apply)(PassingDate.unapply))
  private val orderFeedPage = Redirect(routes.OrderController.toOrderFeedPage(""))
  private val errorRedirect = Redirect(routes.HomeController.index()).flashing("error" -> "Не відома помилка... Мяу.")
  private val logger = Logger("OrderControllerLogger")
  private val format = new SimpleDateFormat("yyyy-MM-dd")


  def toOrdersPageWithNextWeek: PlayAction = Action { implicit request =>
    passDateForm.bindFromRequest.fold(
      _ => errorRedirect,
      Data => {
        toOrdersPage(Data.date, getNextSundayFromGivenDate)
      }
    )
  }

  def toOrdersPageWithLastWeek: PlayAction = Action { implicit request =>
    passDateForm.bindFromRequest.fold(
      _ => errorRedirect,
      Data => toOrdersPage(Data.date, getLastSundayFromGivenDate)
    )
  }

  def toOrderFeedPage(search: String): PlayAction = {
    val messageAndOrderMap = orderModel.getAllTableRows
      .redeemWith(t => {
        logger.error("See stack trace", t)
        IO {
          val emptySeq: Seq[OrderForDisplay] = Seq()
          ("Помилка 502: Я не зміг підключитись до Бази Даних", emptySeq)
        }
      }, s => IO(("", s)))
    Result
    Action { implicit request =>
        messageAndOrderMap.map { case (m: String, r: Seq[OrderForDisplay]) =>
          if (r.isEmpty || m.nonEmpty) Ok(views.html.orders(r, search, Option(m)))
          else Ok(views.html.orders(r, search))
        }
        .unsafeRunSync
    }
  }

  def toOrderEditPage(id: Int): PlayAction = Action { implicit request =>
    orderModel.findBy(id).redeemWith(t => {
      logger.error(s"Can't find order by ID = $id", t)
      IO(orderFeedPage.flashing("error" -> "Помилка 502: Я не зміг підключитись до Бази Даних"))
    }, s => IO {
      Ok(views.html.editOrder(id,
        orderForm.fill(s),
        orderModel.getMenusToolsForAddingToOrder,
        orderModel.getInOrderToTextWithCostMap,
        orderModel.getPayments
      ))
    })
      .unsafeRunSync()
  }

  def toOrderCreatePage: PlayAction = Action { implicit request =>
    Ok(views.html.createOrder(orderForm,
      orderModel.getMenusToolsForAddingToOrder.toList,
      None,
      orderModel.getInOrderToTextWithCostMap,
      orderModel.getPayments))
  }

  def toOrderCreatePageWithCustomerId(id: Int): PlayAction = Action { implicit request =>
    Ok(views.html.createOrder(orderForm,
      orderModel.getMenusToolsForAddingToOrder.toList,
      Option(id),
      orderModel.getInOrderToTextWithCostMap,
      orderModel.getPayments))
  }

  def updateOrder(id: ID): PlayAction = Action { implicit request =>
    orderForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.editOrder(id,
        formWithErrors,
        orderModel.getMenusToolsForAddingToOrder,
        orderModel.getInOrderToTextWithCostMap,
        orderModel.getPayments)),
      orderForEditAndCreate => resultWithFlash(orderFeedPage, orderModel.edit(id, orderForEditAndCreate), "Замовлення змінено, мяу")
    )
  }

  //  def deleteOrder(id: ID): PlayAction = Action { implicit request => // TODO: Need to think on it a bit more
  //
  //  }

  def createOrder: PlayAction = Action { implicit request =>
    orderForm.bindFromRequest.fold(
      formWithErrors => BadRequest(
        views.html.createOrder(formWithErrors,
          orderModel.getMenusToolsForAddingToOrder.toList,
          None,
          orderModel.getInOrderToTextWithCostMap,
          orderModel.getPayments)).flashing("error" -> "Чогось не вистачає"),
      orderForEditAndCreate => resultWithFlash(orderFeedPage, orderModel.insert(orderForEditAndCreate), "Замовлення додано, мяу")
    )
  }

  def generateCourierStickers(date: String): PlayAction = Action { implicit request =>

    Result(
      header = ResponseHeader(200),
      body = HttpEntity.Strict(ByteString(orderModel.generateCourierStickers(format.parse(date)).unsafeRunSync()), Some("application/pdf"))
    )
  }

  private def toOrdersPage(date: Date, convertingDate: Date => String)(implicit mR: MessagesRequestHeader): Result = {
    Ok(views.html.orders(orderModel.getAllOrdersWhere(format.parse(convertingDate(date))).unsafeRunSync(), "", currentDate = convertingDate(date)))
  }

  private def resultWithFlash(result: Result, modelResponse: Boolean, successFlash: String, errorFlash: String = "Щось пішло не так ;("): Result = {
    if (modelResponse) result.flashing("success" -> successFlash)
    else result.flashing("error" -> errorFlash)
  }
}
