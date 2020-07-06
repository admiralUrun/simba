package controllers

import scala.reflect.io.File
import javax.inject.{Inject, Singleton}
import models.{OfferForCreate, OfferModel}
import play.api.data.Forms._
import play.api.data.Form
import play.api.mvc._

@Singleton
class OfferController @Inject()(offerModel: OfferModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc)  {
  type PlayAction = Action[AnyContent]
  private val setOfferForm = ???

  def toOfferSettingsPage: PlayAction = Action { implicit request =>
    Ok(views.html.setOffersForOrders())
  }

  def toSetOfferPage(title: String, menuTitle: String): PlayAction = Action { implicit request =>
    Ok(views.html.setOffer(title, menuTitle))
  }

  def setOffer: PlayAction = Action { implicit request =>
    Ok("")
  }
}
