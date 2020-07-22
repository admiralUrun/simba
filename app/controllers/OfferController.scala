package controllers

import javax.inject.{Inject, Singleton}
import models._
import play.api.data.Forms._
import play.api.data.Form
import play.api.mvc._

@Singleton
class OfferController @Inject()(offerModel: OfferModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
  type PlayAction = Action[AnyContent]
  private val setOfferForm = Form(
    mapping(
      "menuType" -> nonEmptyText,
      "recipesId" -> list(number).verifying(_.nonEmpty)
    )(SettingOffer.apply)(SettingOffer.unapply)
  )
  private val editOfferForm = Form(
    mapping(
      "id" -> list(number),
      "name" -> list(nonEmptyText),
      "price" -> list(number),
      "menuType" -> nonEmptyText
    )(EditOffer.apply)(EditOffer.unapply)
  )

  private val offersPage = Redirect(routes.OfferController.toOffersPage())

  def toOffersPage: PlayAction = Action { implicit request =>
    Ok(views.html.offers())
  }

  def toOfferPage(title: String, menuType: String): PlayAction = Action { implicit request =>
    Ok(views.html.offer(title, menuType))
  }

  def toCreateOfferPage(menuType: String): PlayAction = Action { implicit request =>
    Ok(views.html.createOffer(menuType, setOfferForm))
  }

  def toOfferPreferencePage(title: String, menuType: String): PlayAction = Action { implicit  request =>
    Ok(views.html.editOffer(
      title,
      menuType,
      editOfferForm.fill(offerModel.getOfferPreferencesByMenuType(menuType)))
    )
  }

  def getRecipesForOfferSearch(name: String): PlayAction = Action {
    Ok(offerModel.getRecipesByName(name))
  }

  def setOffer(menuType: String): PlayAction = Action { implicit request =>
    /**
     * Changes in title here should be repeated in offers.scala.html
     * */
    val offerPageMap = Map(
      "classic" -> Redirect(routes.OfferController.toOfferPage("Налаштування Класичного Меню", "classic")),
      "lite" -> Redirect(routes.OfferController.toOfferPage("Налаштування Лайт Меню", "lite")),
      "breakfast" -> Redirect(routes.OfferController.toOfferPage("Налаштування Сніданок Меню", "breakfast")),
      "soup" -> Redirect(routes.OfferController.toOfferPage("Налаштування Суп Меню", "soup")),
      "desert" -> Redirect(routes.OfferController.toOfferPage("Налаштування Десерт Меню", "desert")),
      "promo" -> Redirect(routes.OfferController.toOfferPage("Налаштування Промо Меню", "promo"))
    )
    setOfferForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.createOffer(menuType, formWithErrors)),
      settingOffer => {
        resultWithFlash(offerPageMap(menuType), offerModel.setOffer(settingOffer), "Готово Тепер Встановіть ціни, мяу")
      }
    )
  }

  def editOffer(): PlayAction = Action { implicit request =>
    editOfferForm.bindFromRequest.fold(
      formWithError => BadRequest(views.html.editOffer("Налаштування Пропозиції", formWithError.data("menuType"), formWithError)),
      offerPreferences => {
        resultWithFlash(offersPage, offerModel.setOfferPreferences(offerPreferences), "Пропозицію Зміненно")
      }
    )
  }

  private def resultWithFlash(result: Result, modelResult: Boolean, successFlash: String, errorFlash: String = "Щось пішло не так ;("): Result = {
    if (modelResult) result.flashing("success" -> successFlash)
    else result.flashing("error" -> errorFlash)
  }
}
