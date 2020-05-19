package controllers

import javax.inject._
import models.{OrderModel, PlayOrderForEditAndCreate}
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
      "orderDay" -> date, "deliveryDay" -> date,
      "deliverFrom" -> localTime, "deliverTo" -> localTime,
      "inOrder" -> nonEmptyText,
      "total" -> number,
      "paid" -> boolean, "delivered" -> boolean,
      "note" -> optional(text)
    )(PlayOrderForEditAndCreate.apply)(PlayOrderForEditAndCreate.unapply))
  private val orderFeedPage = Redirect(routes.OrderController.toOrderFeedPage(""))

  def toOrderFeedPage(search: String): PlayAction = Action { implicit request =>
    val orders = orderModel.getAllTableRows
    Ok(views.html.orders(Map(), search))
  }

  def toOrderEditPage(id:Int): PlayAction = Action { implicit request =>
    Ok(views.html.editOrder(id, orderForm)) // TODO fill with order
  }

  def toOrderCreatePage: PlayAction = Action { implicit request =>
    Ok(views.html.createOrder(orderForm))
  }

  def updateOrder(id: Int): PlayAction = Action { implicit request =>
    orderFeedPage
  }

  def createOrder: PlayAction = Action { implicit request =>
    orderFeedPage
  }


}
