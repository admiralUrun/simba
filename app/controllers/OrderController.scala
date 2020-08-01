package controllers

import javax.inject._
import models.{OrderModel, OrderForEditAndCreate}
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
      "orderDay" -> date, "deliveryDay" -> date,
      "deliverFrom" -> nonEmptyText, "deliverTo" -> nonEmptyText,
      "inOrder" -> list(number),
      "total" -> number,
      "payment" -> nonEmptyText,
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
    Ok(views.html.editOrder(id,
      orderForm.fill(orderModel.findById(id)),
      orderModel.getMenusToolsForAddingToOrder,
      orderModel.getInOrderToTextWithCostMap))
  }

  def toOrderCreatePage: PlayAction = Action { implicit request =>
    Ok(views.html.createOrder(orderForm, orderModel.getMenusToolsForAddingToOrder, None, orderModel.getInOrderToTextWithCostMap))
  }

  def toOrderCreatePageWithCustomerId(id: Int): PlayAction = Action { implicit request =>
    Ok(views.html.createOrder(orderForm, orderModel.getMenusToolsForAddingToOrder, Option(id), orderModel.getInOrderToTextWithCostMap))
  }

  def updateOrder(id: ID): PlayAction = Action { implicit request =>
    orderForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.editOrder(id,
        formWithErrors,
        orderModel.getMenusToolsForAddingToOrder,
        orderModel.getInOrderToTextWithCostMap)),
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
        orderModel.getMenusToolsForAddingToOrder,
          None,
          orderModel.getInOrderToTextWithCostMap)),
      orderForEditAndCreate => resultWithFlash(orderFeedPage, orderModel.insert(orderForEditAndCreate), "Замовлення додано, мяу")
    )
  }

  private def resultWithFlash(result: Result, modelResponse: Boolean, successFlash: String, errorFlash: String = "Щось пішло не так ;("): Result = {
    if(modelResponse) result.flashing("success" -> successFlash)
    else result.flashing("error" -> errorFlash)
  }
}
