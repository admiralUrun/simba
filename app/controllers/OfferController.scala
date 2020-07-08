package controllers

import javax.inject.{Inject, Singleton}
import models._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.Files
import play.api.mvc._
import scala.reflect.io.File

@Singleton
class OfferController @Inject()(offerModel: OfferModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
  type PlayAction = Action[AnyContent]
  type PlayActionWithFile = Action[MultipartFormData[Files.TemporaryFile]]
  private val preferenceOfferForm = Form(
    mapping(
      "names" -> list(nonEmptyText),
      "prices" -> list(number),
      "menuType" -> nonEmptyText
    )(OfferPreferences.apply)(OfferPreferences.unapply)
  )

  private val offersPage = Redirect(routes.OfferController.toOffersPage())

  def toOffersPage: PlayAction = Action { implicit request =>
    Ok(views.html.offers())
  }

  def toOfferPage(title: String, menuType: String): PlayAction = Action { implicit request =>
    val translator = Map(
      "classic" -> "Класичне",
      "lite" -> "Лайт",
      "breakfast" -> "Сніданок",
      "soup" -> "Суп",
      "desert" -> "Десерт"
    )
      Ok(views.html.offer(title,
        menuType,
        translator.getOrElse(menuType, throw UninitializedFieldError(s"Error can't translate $menuType")))
      )
  }

  def toSetOfferPage(title: String, menuType: String): PlayAction = Action { implicit request =>
    Ok(views.html.setOfferPage(title, menuType))
  }

  def setOffer(title: String, menuType: String): PlayActionWithFile = Action(parse.multipartFormData) { implicit request =>
    request.body
      .file("resepies")
        .map{ resepis =>
          val file = resepis.ref.path.toFile
          resultWithFlash(offerModel.setOffer(OfferForCreate(menuType, file)), "Пропозицію встановлино, мяу")
        }.getOrElse(Redirect(routes.OfferController.toSetOfferPage(title, menuType)).flashing("error" -> "З файлом щось не так"))
  }

  def toOfferPreferencePage(title: String, menuType: String): PlayAction = Action {
    Ok(views.html.pereferanceOfferTemplate(
      title,
      preferenceOfferForm.fill(offerModel.getOfferPreferencesByMuneTupe(menuType)),
      menuType)
    )
  }

  def editOfferPreference(): PlayAction = Action { implicit request =>
    preferenceOfferForm.bindFromRequest.fold(
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
