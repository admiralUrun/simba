package models

import java.util.Date
import cats.effect.IO
import dao.Dao
import javax.inject.{Inject, Singleton}
import services.SimbaHTMLHelper.convertMenuTypeToString
import services.SimbaAlias._

@Singleton
class OfferModel @Inject()(dao: Dao) {

  def getOfferPreferencesByMenuType(menuType: Int, executionDate: Date): EditOffer = {
    val offers = dao.getOfferByDateAndMenuType(executionDate, menuType).unsafeRunSync().toList
    EditOffer(offers.map(_.id.head), offers.map(_.name), offers.map(_.price), executionDate, menuType)
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

    def validationError(menuType: Int, howManyIds: Int): Boolean = Map(
      6 -> (howManyIds == 3),
      5 -> (howManyIds >= 1),
      4 -> (howManyIds >= 1),
      3 -> (howManyIds == 5),
      2 -> (howManyIds == 5),
      1 -> (howManyIds == 5)
    )(menuType)

    def primeMenuTypeInsets(menuType: Int, recipes: List[Recipe]): List[InsertOffer] = {
      val recipesWithIndex = recipes.zipWithIndex
      val allRecipesOnFour = recipesWithIndex.map { case (r, i) =>
        InsertOffer(s"${convertMenuTypeToString(menuType)} ${i + 1} на 4", 0, List(r), 4)
      }
      val allRecipesOnTwo = recipesWithIndex.map { case (r, i) =>
        InsertOffer(s"${convertMenuTypeToString(menuType)} ${i + 1} на 2", 0, List(r), 2)
      }

      List( // TODO: Think of something better
        InsertOffer(s"5 на 4 ${convertMenuTypeToString(menuType)}", standardTitleToPrice(s"5 на 4 ${convertMenuTypeToString(menuType)}"), recipes, 4),
        InsertOffer(s"5 на 2 ${convertMenuTypeToString(menuType)}", standardTitleToPrice(s"5 на 2 ${convertMenuTypeToString(menuType)}"), recipes, 2),
        InsertOffer(s"3 на 4 ${convertMenuTypeToString(menuType)}", standardTitleToPrice(s"3 на 4 ${convertMenuTypeToString(menuType)}"), recipes.take(3), 4),
        InsertOffer(s"3 на 2 ${convertMenuTypeToString(menuType)}", standardTitleToPrice(s"3 на 2 ${convertMenuTypeToString(menuType)}"), recipes.take(3), 2)
      ) ::: allRecipesOnTwo ::: allRecipesOnFour
    }

    def sideMenuTypeInsets(recipes: List[Recipe]): List[InsertOffer] = {
      recipes.map(r => InsertOffer(r.name, 0, List(r), 2))
    }

    def promoMenuTypeInsets(recipes: List[Recipe]): List[InsertOffer] = {
      List(
        InsertOffer("Промо на 2", 0, recipes, 2),
        InsertOffer("Промо на 4", 0, recipes, 4)
      )
    }

    val menuType = sO.menuType

    if (!validationError(menuType, sO.recipeIds.length)) false
    else {
      val recipes = dao.getRecipesBy(sO.recipeIds).unsafeRunSync().toList
      val insertOffers = {
        if (menuType == 4 || menuType == 5) sideMenuTypeInsets(recipes)
        else if (menuType == 6) promoMenuTypeInsets(recipes)
        else primeMenuTypeInsets(menuType, recipes)
      }

      dao.insertOrUpdateOffers(sO.executionDate, menuType, insertOffers).redeemWith(_ => IO(false), _ => IO(true)).unsafeRunSync()
    }
  }

  def setOfferPreferences(editOffer: EditOffer): Boolean = {
    if (editOffer.ids.length != editOffer.prices.length || editOffer.ids.length != editOffer.names.length) false
    else {
      dao.updateOffersNameAndPrice(editOffer.ids.zip(editOffer.names.zip(editOffer.prices))).redeemWith(_ => IO(false), _ => IO(true)).unsafeRunSync()
    }
  }

  def getRecipesByName(name: String): Seq[Recipe] = {
    dao.getRecipesLike(name).unsafeRunSync()
  }

  def getAllRecipesOnThisWeek(date: Date): List[Recipe] = { // TODO
    List()
  }

}

case class Offer(id: Option[ID], name: String, price: Int, executionDate: Date, menuType: Int)

case class Recipe(id: Option[ID], name: String, menuType: String, edited: Boolean)

case class OfferRecipes(offerId: ID, recipesId: ID, quantity: Int)

case class SettingOffer(menuType: Int, executionDate: Date, recipeIds: List[ID])

case class EditOffer(ids: List[ID], names: List[String], prices: List[Int], executionDate: Date, menuType: Int)

case class PassingDate(date: Date)

case class InsertOffer(name: String, price: Int, recipes: List[Recipe], quantityOfRecipes: Int)
