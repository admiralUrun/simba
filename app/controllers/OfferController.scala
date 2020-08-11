package controllers

import java.text.SimpleDateFormat

import javax.inject.{Inject, Singleton}
import models._
import play.api.data.Forms._
import play.api.data.Form
import play.api.mvc._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Writes}
import services.SimbaHTMLHelper.{getLastSundayFromGivenDate, getNextSundayFromGivenDate}

@Singleton
class OfferController @Inject()(offerModel: OfferModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
  type PlayAction = Action[AnyContent]
  private val setOfferForm = Form(
    mapping(
      "menuType" -> nonEmptyText,
      "executionDate" -> date,
      "recipesId" -> list(number).verifying(_.nonEmpty)
    )(SettingOffer.apply)(SettingOffer.unapply)
  )
  private val editOfferForm = Form(
    mapping(
      "id" -> list(number),
      "name" -> list(nonEmptyText),
      "price" -> list(number),
      "date" -> date,
      "menuType" -> nonEmptyText
    )(EditOffer.apply)(EditOffer.unapply)
  )
  private val passDateForm = Form(
    mapping(
      "date" -> date
    )(PassingDate.apply)(PassingDate.unapply)
  )

  private implicit val recipesWriter: Writes[Recipe] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "menuType").write[String] and
      (JsPath \ "edited").write[Boolean]
    ) (unlift(Recipe.unapply))

  private val offersPage = Redirect(routes.OfferController.toOffersPage())
  private val errorRedirect = Redirect(routes.HomeController.index()).flashing("error" -> "Не треба тут сувати мені відредарований HTML!")

  def toOffersPage: PlayAction = Action { implicit request =>
    Ok(views.html.offers())
  }

  def toOfferPageWithNextWeek: PlayAction = Action { implicit request =>
    passDateForm.bindFromRequest.fold(
      _ => errorRedirect,
      Data => Ok(views.html.offers(getNextSundayFromGivenDate(Data.date)))
    )
  }

  def toOfferPageWithLastWeek: PlayAction = Action { implicit request =>
    passDateForm.bindFromRequest.fold(
      _ => errorRedirect,
      Data => Ok(views.html.offers(getLastSundayFromGivenDate(Data.date)))
    )
  }

  def toOfferPage(title: String, menuType: String): PlayAction = Action { implicit request =>
    passDateForm.bindFromRequest.fold(
      _ => errorRedirect,
      Date => Ok(views.html.offer(title, Date.date, menuType))
    )
  }

  def toCreateOfferPage(menuType: String): PlayAction = Action { implicit request =>
    passDateForm.bindFromRequest.fold(
      _ => errorRedirect,
      Date => Ok(views.html.createOffer(menuType, Date.date, setOfferForm))
    )
  }

  def toOfferPreferencePage(title: String, menuType: String): PlayAction = Action { implicit request =>
    passDateForm.bindFromRequest.fold(
      _ => errorRedirect,
      Date => Ok(views.html.editOffer(title, Date.date, menuType,
        editOfferForm.fill(offerModel.getOfferPreferencesByMenuType(menuType, Date.date)))
      )
    )
  }

  def toRecipesOnThisWeekPage: PlayAction = Action { implicit request =>
    passDateForm.bindFromRequest.fold(
      _ => errorRedirect,
      Date => Ok(views.html.recipesOnThisWeek(offerModel.getAllRecipesOnThisWeek(Date.date)))
    )
  }

  def getRecipesForOfferSearch(name: String): PlayAction = Action {
    Ok(Json.toJson(offerModel.getRecipesByName(name)))
  }

  def setOffer(menuType: String): PlayAction = Action { implicit request =>
    setOfferForm.bindFromRequest.fold(
      formWithErrors => {
        val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
        val executionDateFromFiled = formWithErrors("executionDate").value.head
        BadRequest(views.html.createOffer(menuType,
          dateFormat.parse(executionDateFromFiled),
          formWithErrors))
      },
      settingOffer => {
        val date = settingOffer.executionDate
        /**
         * Changes in title here should be repeated in offers.scala.html
         **/
        val offerPageMap = Map(
          "classic" -> Ok(views.html.offer("Налаштування Класичного Меню", date, "classic")),
          "lite" -> Ok(views.html.offer("Налаштування Лайт Меню", date, "lite")),
          "breakfast" -> Ok(views.html.offer("Налаштування Сніданок Меню", date, "breakfast")),
          "soup" -> Ok(views.html.offer("Налаштування Суп Меню", date, "soup")),
          "desert" -> Ok(views.html.offer("Налаштування Десерт Меню", date, "desert")),
          "promo" -> Ok(views.html.offer("Налаштування Промо Меню", date, "promo"))
        )
        resultWithFlash(offerPageMap(menuType), offerModel.setOffer(settingOffer), "Готово Тепер Встановіть ціни, мяу") // TODO: Flashing doesn't work think on how to fix it
      }
    )
  }

  def editOffer(): PlayAction = Action { implicit request =>
    editOfferForm.bindFromRequest.fold(
      formWithError => BadRequest(views.html.editOffer("Налаштування Пропозиції", formWithError.value.head.executionDate, formWithError.data("menuType"), formWithError)),
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
