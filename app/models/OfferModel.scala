package models

import java.util.Date

import Dao.Dao
import cats.effect.IO
import cats.implicits._
import javax.inject.{Inject, Singleton}
import services.SimbaHTMLHelper.translateMenuType // Maybe isn't a good a idea to use it here just don't want to duplicate code

@Singleton
class OfferModel @Inject()(dao: Dao) {

  def getOfferPreferencesByMenuType(menuType: String, executionDate: Date): EditOffer = {
    val offers = dao.getOfferByDateAndMenuType(executionDate, menuType).unsafeRunSync().toList
    EditOffer(offers.map(_.id.head), offers.map(_.name), offers.map(_.price), executionDate, menuType)
  }

  def setOfferPreferences(editOffer: EditOffer): Boolean = {
    if(editOffer.ids.length != editOffer.prices.length || editOffer.ids.length != editOffer.names.length) false
    else {
      dao.editOffers(editOffer.ids.zip(editOffer.names.zip(editOffer.prices))).unsafeRunSync()
      true
    }
  }

  def getRecipesByName(name: String): Seq[Recipe] = {
    dao.getRecipesLike(name).unsafeRunSync()
  }


  def setOffer(sO: SettingOffer): Boolean = {
    val standardTitleToPrice: Map[String, Int] = Map(
      "5 на 4 Класичне" -> 2249,
      "5 на 2 Класичне" -> 1289,
      "3 на 2 Класичне" -> 849,
      "3 на 4 Класичне" -> 1489,
      "5 на 4 Лайт" -> 2249,
      "5 на 2 Лайт" -> 1289,
      "3 на 2 Лайт" -> 849,
      "3 на 4 Лайт" -> 1489,
      "5 на 4 Сніданок" -> 1589,
      "5 на 2 Сніданок" -> 849,
      "3 на 2 Сніданок" -> 549,
      "3 на 4 Сніданок" -> 989,
    )

    def validationError(menuType: String, howManyIds: Int): Boolean = Map(
        "promo" -> (howManyIds == 3),
        "soup" -> (howManyIds >= 1),
        "desert" -> (howManyIds >= 1),
        "classic" -> (howManyIds == 5),
        "lite" -> (howManyIds == 5),
        "breakfast" -> (howManyIds == 5)
      )(menuType)

    def primeMenuTypeInsets(menuType: String, recipes: List[Recipe]): List[InsertOffer] = {
      val recipesWithIndex = recipes.zipWithIndex
      val allRecipesOnFour = recipesWithIndex.map{ case (r, i) =>
        InsertOffer(s"${translateMenuType(menuType)} ${i + 1} на 4", 0, List(r), 4)
      }
      val allRecipesOnTwo = recipesWithIndex.map { case (r, i) =>
        InsertOffer(s"${translateMenuType(menuType)} ${i + 1} на 2", 0, List(r), 2)
      }

      List( // TODO: Think of something better
        InsertOffer(s"5 на 4 ${translateMenuType(menuType)}", standardTitleToPrice(s"5 на 4 ${translateMenuType(menuType)}"), recipes, 4),
        InsertOffer(s"5 на 2 ${translateMenuType(menuType)}", standardTitleToPrice(s"5 на 2 ${translateMenuType(menuType)}"), recipes, 2),
        InsertOffer(s"3 на 4 ${translateMenuType(menuType)}", standardTitleToPrice(s"3 на 4 ${translateMenuType(menuType)}"), recipes.take(3), 4),
        InsertOffer(s"3 на 2 ${translateMenuType(menuType)}", standardTitleToPrice(s"3 на 2 ${translateMenuType(menuType)}"), recipes.take(3), 2)
      ) ::: allRecipesOnTwo ::: allRecipesOnFour
    }

    def sideMenuTypeInsets(menuType: String, recipes: List[Recipe]): List[InsertOffer] = {
      recipes.map(r => InsertOffer(r.name, 0, List(r), 2))
    }

    def promoMenuTypeInsets(menuType: String, recipes: List[Recipe]): List[InsertOffer] = {
      List(
        InsertOffer("Промо на 2", 0, recipes, 2),
          InsertOffer("Промо на 4", 0, recipes, 4)
      )
    }

    val menuType = sO.menuType

    if(!validationError(menuType, sO.recipeIds.length)) false
    else {
      val recipes = dao.getRecipesBy(sO.recipeIds).unsafeRunSync().toList
      val insertOffers = {
        if (menuType == "soup" || menuType == "desert") sideMenuTypeInsets(menuType, recipes)
        else if (menuType == "promo") promoMenuTypeInsets(menuType, recipes)
        else primeMenuTypeInsets(menuType, recipes)
      }

      dao.insertOffers(sO.executionDate, sO.menuType, insertOffers).unsafeRunSync()
      /**
       *  Returning true for a version with out unsafeRun in Controller
       * */
      true
    }
  }

  def getAllRecipesOnThisWeek(date: Date): List[Recipe] = { // TODO
    List()
  }

}

case class Offer(id: Option[Int], name: String, price: Int, executionDate: Date, menuType: String)

case class Recipe(id: Option[Int], name: String, menuType: String, edited: Boolean)

case class OfferRecipes(offerId: Int, recipesId: Int, quantity: Int)

case class SettingOffer(menuType: String, executionDate: Date,  recipeIds: List[Int])

case class EditOffer(ids: List[Int], names: List[String], prices: List[Int], executionDate: Date, menuType: String)

case class PassingDate(date: Date)

case class InsertOffer(name: String, price: Int, recipes: List[Recipe], quantityOfRecipes: Int)
