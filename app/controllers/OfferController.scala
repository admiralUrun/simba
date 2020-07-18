package controllers

import javax.inject.{Inject, Singleton}
import models._
import play.api.data.Forms._
import play.api.data.Form
import play.api.mvc._

@Singleton
class OfferController @Inject()(offerModel: OfferModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
  type PlayAction = Action[AnyContent]
  private val offerForm = Form(
    mapping(
      "ids" -> list(number),
      "pricesForTwo" -> list(number),
      "menuType" -> nonEmptyText
    )(OfferPreferences.apply)(OfferPreferences.unapply)
  )

  private val offersPage = Redirect(routes.OfferController.toOffersPage())

  def toOffersPage: PlayAction = Action { implicit request =>
    Ok(views.html.offers())
  }

  def toOfferPage(title: String, menuType: String): PlayAction = Action { implicit request =>
      Ok(views.html.offer(title, menuType))
  }

  def toSetOfferPage(title: String, menuType: String): PlayAction = Action { implicit request =>
    Ok(views.html.setOfferPage(title, menuType))
  }

  def setOffer(title: String, menuType: String): PlayAction = Action { implicit request =>
    Ok("Working on")
  }

  def toOfferPreferencePage(title: String, menuType: String): PlayAction = Action {
    Ok(views.html.pereferanceOfferTemplate(
      title,
      offerForm.fill(offerModel.getOfferPreferencesByMuneTupe(menuType)),
      menuType)
    )
  }

  def editOfferPreference(): PlayAction = Action { implicit request =>
    offerForm.bindFromRequest.fold(
      formWithError => BadRequest(views.html.pereferanceOfferTemplate("Налаштування Пропозиції", formWithError, formWithError.data("menuType"))),
      offerPreferences => {
        resultWithFlash(offerModel.setOfferPreferences(offerPreferences), "Пропозицію Зміненно")
      }
    )
  }

  private def resultWithFlash(modelResult: Boolean, successFlash: String, errorFlash: String = "Щось пішло не так ;("): Result = {
    if (modelResult) offersPage.flashing("success" -> successFlash)
    else offersPage.flashing("error" -> errorFlash)
  }
}
