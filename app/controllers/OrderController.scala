package controllers

import javax.inject._
import models.{OrderModel, OrderForEditAndCreate}
import play.api.data.Forms._
import play.api.data.Form
import play.api.mvc._

@Singleton
class OrderController @Inject()(orderModel: OrderModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
  type PlayAction = Action[AnyContent]
  private val orderForm = Form(
    mapping(
      "id" -> ignored(None: Option[Int]),
      "customerId" -> number,
      "addressId" -> number,
      "orderDay" -> date, "deliveryDay" -> date,
      "deliverFrom" -> nonEmptyText, "deliverTo" -> nonEmptyText,
      "inOrder" -> list(number),
      "total" -> number,
      "offlineDelivery" -> boolean, "deliveryOnMonday" -> boolean,
      "paid" -> boolean, "delivered" -> boolean,
      "note" -> optional(text)
    )(OrderForEditAndCreate.apply)(OrderForEditAndCreate.unapply))
  private val orderFeedPage = Redirect(routes.OrderController.toOrderFeedPage(""))

  def toOrderFeedPage(search: String): PlayAction = Action { implicit request =>
    val orders = orderModel.getAllTableRows
    Ok(views.html.orders(orders, search))
  }

  def toOrderEditPage(id: Int): PlayAction = Action { implicit request =>
    val o = orderModel.findById(id)
    val oF = orderForm.fill(o)
    Ok(views.html.editOrder(id,
      oF,
      orderModel.getMenusToolsForAddingToOrder,
      orderModel.getInOrderToTextWithCostMap))
  }

  def toOrderCreatePage: PlayAction = Action { implicit request =>
    Ok(views.html.createOrder(orderForm, orderModel.getMenusToolsForAddingToOrder, None))
  }

  def toOrderCreatePageWithCustomerId(id: Int): PlayAction = Action { implicit request =>
    Ok(views.html.createOrder(orderForm, orderModel.getMenusToolsForAddingToOrder, Option(id)))
  }

  def updateOrder(id: Int): PlayAction = Action { implicit request =>
    orderForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.editOrder(id, formWithErrors, orderModel.getMenusToolsForAddingToOrder, orderModel.getInOrderToTextWithCostMap)),
      orderForEditAndCreate => resultWithFlash(orderFeedPage, orderModel.edit(orderForEditAndCreate), "Замовлення змінено, мяу")
    )
  }

  def createOrder: PlayAction = Action { implicit request =>
    orderForm.bindFromRequest.fold(
      formWithErrors => BadRequest(
        views.html.createOrder(formWithErrors, // FIX bug with not saving a form in it failed
        orderModel.getMenusToolsForAddingToOrder,
        None)),
      orderForEditAndCreate => resultWithFlash(orderFeedPage, orderModel.insert(orderForEditAndCreate), "Замовлення додано, мяу")
    )
  }

  private def resultWithFlash(result: Result, modelResponse: Boolean, successFlash: String, errorFlash: String = "Щось пішло не так ;("): Result = {
    if(modelResponse) result.flashing("success" -> successFlash)
    else result.flashing("error" -> errorFlash)
  }
}
