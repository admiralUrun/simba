package controllers

import javax.inject._
import models._
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
    )(PlayOrderForEditAndCreate.apply)(PlayOrderForEditAndCreate.unapply)
  )

  def toOrderListPage(search: String): PlayAction = Action { implicit request =>
    val orders = orderModel.getAllTableRows
    Ok(views.html.orders(Map(), search))
  }

}
