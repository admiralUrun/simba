package controllers

import scala.reflect.io.File
import javax.inject.{Inject, Singleton}
import models._
import play.api.data.Forms._
import play.api.data.Form
import play.api.mvc._

@Singleton
class OfferController @Inject()(offerModel: OfferModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc)  {
  type PlayAction = Action[AnyContent]
  private val preferenceOfferForm = Form(
    mapping(
      "names" -> list(nonEmptyText),
      "prices" -> list(number),
      "menuType" -> nonEmptyText
    )(OfferPreferences.apply)(OfferPreferences.unapply)
  )

  def toOfferSettingsPage: PlayAction = Action { implicit request =>
    Ok("")
  }

  def toSetOfferPage(title: String, menuType: String): PlayAction = Action { implicit request =>
    Ok("")
  }

  def setOffer: PlayAction = Action { implicit request =>
    Ok("")
  }
}
