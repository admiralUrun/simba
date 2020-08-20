package controllers

import javax.inject._
import models.{OrderModel, OrderInput}
import play.api.data.Forms._
import play.api.data.Form
import play.api.mvc._
import services.SimbaAlias.ID

@Singleton
class OrderController @Inject()(orderModel: OrderModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
  type PlayAction = Action[AnyContent]
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
  private val orderFeedPage = Redirect(routes.OrderController.toOrderFeedPage(""))

  def toOrderFeedPage(search: String): PlayAction = Action { implicit request =>
    Ok(views.html.orders(orderModel.getAllTableRows, search))
  }

  def toOrderEditPage(id: Int): PlayAction = Action { implicit request =>
    Ok(views.html.editOrder(id,
      orderForm.fill(orderModel.findBy(id)),
      orderModel.getMenusToolsForAddingToOrder,
      orderModel.getInOrderToTextWithCostMap,
      orderModel.getPayments
    ))
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
          orderModel.getPayments)),
      orderForEditAndCreate => resultWithFlash(orderFeedPage, orderModel.insert(orderForEditAndCreate), "Замовлення додано, мяу")
    )
  }

  private def resultWithFlash(result: Result, modelResponse: Boolean, successFlash: String, errorFlash: String = "Щось пішло не так ;("): Result = {
    if (modelResponse) result.flashing("success" -> successFlash)
    else result.flashing("error" -> errorFlash)
  }
}
