package controllers

import java.text.SimpleDateFormat
import java.util.Date
import javax.inject._
import play.api.Logger
import play.api.http.HttpEntity
import org.joda.time.DateTime
import org.json4s.jackson.Serialization._
import play.api.mvc._
import zio.ZIO
import akka.util.ByteString
import services.SimbaAlias._
import models.{OrderInput, OrderModel}
import org.joda.time.format.DateTimeFormat
import services.SimbaAction.ActionBuilderOps

@Singleton
class OrderController @Inject()(orderModel: OrderModel, cc: ControllerComponents) extends BaseSimbaController(cc) {
	private val logger = Logger("OrderControllerLogger")

	def ordersOnThisWeek: PlayAction = Action.zio(parse.default) { _ =>
		orderModel
			.getAllOrdersWhere(DateTime.now())
			.map(os => Ok(write(os)).as(JSON))
			.catchAll(t => {
				logger.error("", t)
				ZIO(BadRequest(""))
			})
	}

	def orderForDate: PlayAction = Action.zio(parse.default) { implicit req =>
		extractClassFromJson("date")
			.map(DateTime.parse)
			.flatMap(orderModel.getAllOrdersWhere)
			.map(os =>Ok(write(os)).as(JSON))
	}

	def findBy(id: Int): PlayAction = Action.zio(parse.default) { implicit request =>
		orderModel
      .findBy(id)
			.map(o => Ok(write(o)).as(JSON))
			.catchAll(t => {
				logger.error("", t)
				ZIO(BadRequest(""))
			})
	}


  def getMenu: PlayAction = Action.zio(parse.default) { _ =>
    orderModel
      .getMenusToolsForAddingToOrder
      .map(m => Ok(write(m)).as(JSON))
      .catchAll(t => {
        logger.error("", t)
        ZIO(BadRequest(""))
      })
  }

	//  def toOrderCreatePage: PlayAction = Action { implicit request =>
	//    Ok(views.html.createOrder(orderForm,
	//      orderModel.getMenusToolsForAddingToOrder.toList,
	//      None,
	//      orderModel.getInOrderToTextWithCostMap,
	//      orderModel.getPayments))
	//  }

	def updateOrder: PlayAction = Action.zio(parse.default) { implicit req =>
		extractClassFromJson("order")
			.map(read[OrderInput])
			.flatMap(orderModel.edit)
			.as(Ok("SAVED"))
			.catchAll(t => {
				logger.error("", t)
				ZIO(BadRequest("Error"))
			})
	}

	def createOrder: PlayAction = Action.zio(parse.default) { implicit req =>
		extractClassFromJson("order")
			.map(read[OrderInput])
			.flatMap(orderModel.insert)
			.as(Ok("SAVED"))
			.catchAll(t => {
				logger.error("", t)
				ZIO(BadRequest("Error"))
			})
	}

	//  def deleteOrder(id: ID): PlayAction = Action { implicit request => // TODO: Need to think on it a bit more
	//
	//  }

	//  def generateCourierStickers(date: String): PlayAction = Action { implicit request =>
	//    Result(
	//      header = ResponseHeader(200),
	//      body = HttpEntity.Strict(ByteString(orderModel.generateCourierStickers(format.parse(date)).unsafeRunSync()), Some("application/pdf"))
	//    )
	//  }
}
