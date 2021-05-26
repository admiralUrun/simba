package controllers

import javax.inject._
import play.api.mvc._
import org.json4s.jackson.Serialization._
import models._
import play.api.Logger
import services.SimbaAction.ActionBuilderOps
import services.SimbaAlias._
import zio.ZIO

@Singleton
class CustomerController @Inject()(customerModel: CustomerModel, cc: ControllerComponents)
  extends BaseSimbaController(cc) {
  private val logger = Logger("CustomerLogger")

  def getCustomerTabs: PlayAction = Action.zio(parse.default) { _ =>
    customerModel.getAllCustomerTableRows
      .map(cs => Ok(write(cs)).as(JSON))
      .catchAll(t => {
        logger.error("", t)
        ZIO(BadRequest(""))
      })
  }

  def getCustomers(search: String): PlayAction = Action.zio(parse.default) { _ =>
    customerModel.getAllCustomerTableRowsLike(search)
      .map(cs => Ok(write(cs)).as(JSON))
      .catchAll(t => {
        logger.error("", t)
        ZIO(BadRequest(""))
      })
  }

  def getCustomer(id: Int): PlayAction = Action.zio(parse.default) { _ =>
    customerModel.findBy(id)
      .map(cs => Ok(write(cs)).as(JSON))
      .catchAll(t => {
        logger.error("", t)
        ZIO(BadRequest(""))
      })
  }

  def getStreets(search: String): PlayAction = Action.zio(parse.default) { _ =>
    customerModel.getStreets(search)
      .map(ss => Ok(write(ss)).as(JSON))
      .catchAll(t => {
        logger.error("", t)
        ZIO(BadRequest(""))
      })
  }

  def update(): PlayAction = Action.zio(parse.default) { implicit request =>
    extractClassFromJson("customerInput")
      .map(read[CustomerInput])
      .flatMap(c => customerModel.editCustomer(c))
      .as(Ok("SAVED"))
      .catchAll(t => {
        logger.error("", t)
        ZIO(BadRequest("Error"))
      })
  }

  def createCustomer: PlayAction = Action.zio(parse.default) { implicit request =>
    extractClassFromJson("customerInput")
      .map(read[CustomerInput])
      .flatMap(c => customerModel.insert(c))
      .as(Ok("SAVED"))
      .catchAll(t => {
        logger.error("", t)
        ZIO(BadRequest("Error"))
      })
  }

}
