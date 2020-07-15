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
      "customerID" -> number,
      "addressID" -> number,
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

  def toOrderEditPage(id:Int): PlayAction = Action { implicit request =>
    Ok(views.html.editOrder(id, orderForm.fill(orderModel.findById(id)), orderModel.getMenusToolsForAddingToOrder, orderModel.getInOrderToTextWithCostMap))
  }

  def toOrderCreatePage: PlayAction = Action { implicit request =>
    Ok(views.html.createOrder(orderForm, orderModel.getMenusToolsForAddingToOrder, None))
  }
  def toOrderCreatePageWithCustomerId(id: Int): PlayAction = Action { implicit request =>
    Ok(views.html.createOrder(orderForm, orderModel.getMenusToolsForAddingToOrder, Option(id)))
  }

  def updateOrder(id: Int): PlayAction = Action { implicit request =>
    orderFeedPage
  }

  def createOrder: PlayAction = Action { implicit request =>
    orderFeedPage
  }
}
