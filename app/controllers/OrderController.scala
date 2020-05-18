package controllers

import javax.inject._
import models.{Order, OrderModel}
import play.api.data.Forms._
import play.api.data.Form
import play.api.mvc._

@Singleton
class OrderController @Inject()(orderModel: OrderModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
  type PlayAction = Action[AnyContent]
  private val orderForm = Form(
    mapping(
      "id" -> ignored(None: Option[Int]),
      "customerID" -> number, // TODO under question
      "orderDay" -> date, "deliveryDay" -> date,
      "deliverFrom" -> localTime, "deliverTo" -> localTime,
      "total" -> number,
      "paid" -> boolean, "delivered" -> boolean,
      "note" -> optional(text)
    )(Order.apply)(Order.unapply)
  )

  def toOrderListPage(search: String): PlayAction = Action { implicit request =>
    Ok(views.html.orders(orderModel.getAllTableRows, search))
  }

}
