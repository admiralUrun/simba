package models

import java.util.Date
import cats.effect.IO
import doobie._
import doobie.implicits._
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.SimbaHTMLHelper.translateMenuType // Maybe isn't a good a idea to use it here just don't want to duplicate code

@Singleton
class OfferModel @Inject()(dS: DoobieStore) {
  protected val xa: DataSourceTransactor[IO] = dS.getXa()
  private implicit val recipesWriter: Writes[Recipe] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "menuType").write[String] and
      (JsPath \ "edited").write[Boolean]
  )(unlift(Recipe.unapply))
  def getOfferPreferencesByMenuType(menuType: String, executionDate: Date): EditOffer = {
    val offers = sql"select * from offers where execution_date = $executionDate and  menu_type = $menuType"
      .query[Offer]
      .to[List]
      .transact(xa)
      .unsafeRunSync()

    EditOffer(offers.map(_.id.head), offers.map(_.name), offers.map(_.price), executionDate, menuType)
  }

  def setOfferPreferences(editOffer: EditOffer): Boolean = {
    if(editOffer.ids.length != editOffer.prices.length || editOffer.ids.length != editOffer.names.length) false
    else {
      editOffer.ids.zip(editOffer.names.zip(editOffer.prices)).traverse { case (id, (name, price)) =>
        sql"update offers set name = $name, price= $price where id= $id".update.run
      }.transact(xa).unsafeRunAsyncAndForget()
      true
    }
  }

  def getRecipesByName(name: String): JsValue = {
    val recipes: List[Recipe] = sql"select * from recipes where name like $name".query[Recipe].to[List].transact(xa).unsafeRunSync()
    Json.toJson(recipes)
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

    def insertOffer(name: String, date: Date, menuType: String, recipes: List[Recipe], quantityOfRecipes: Int): ConnectionIO[Unit] = {
      val price = standardTitleToPrice.getOrElse(name, 0)
      for {
        _ <- sql"insert into offers (name, price, execution_date, menu_type) values ($name, $price, $date, $menuType)".update.run
        id <- sql"select LAST_INSERT_ID()".query[Int].unique
        _ <- recipes.traverse { r =>
          sql"insert into offer_recipes (offer_id, recipe_id, quantity) values ($id, ${r.id}, $quantityOfRecipes)".update.run
        }
      } yield ()
    }

    def primeMenuTypeInsets(menuType: String, recipes: List[Recipe]): IO[List[Unit]] = {
      val recipesWithIndex = recipes.zipWithIndex
      val allRecipesOnFour = recipesWithIndex.map{ case (r, i) =>
        (s"${translateMenuType(menuType)} ${i + 1} на 4", 4, List(r))
      }
      val allRecipesOnTwo = recipesWithIndex.map{ case (r, i) =>
        (s"${translateMenuType(menuType)} ${i + 1} на 2", 2, List(r))
      }

      val list: List[(String, Int, List[Recipe])] = List( // TODO: Think of something better
        (s"5 на 4 ${translateMenuType(menuType)}", 4, recipes),
        (s"5 на 2 ${translateMenuType(menuType)}", 2, recipes),
        (s"3 на 4 ${translateMenuType(menuType)}", 4, recipes.take(3)),
        (s"3 на 2 ${translateMenuType(menuType)}", 2, recipes.take(3))
      ) ::: allRecipesOnFour ::: allRecipesOnTwo

      (deleteBeforeInsets(sO.executionDate, menuType) *>
      insertIntoRecipesByWeek(recipes, sO.executionDate, menuType) *>
      list.traverse{ case (name, quantityOfRecipes, recipes) =>
        insertOffer(name, sO.executionDate, menuType, recipes, quantityOfRecipes)
      }).transact(xa)
    }

    def sideMenuTypeInsets(menuType: String, recipes: List[Recipe]): IO[List[Unit]] = {
      (deleteBeforeInsets(sO.executionDate, menuType) *>
        insertIntoRecipesByWeek(recipes, sO.executionDate, menuType) *>
      recipes.traverse{ r =>
        insertOffer(s"${r.name}", sO.executionDate, menuType, List(r), 2)
      }).transact(xa)
    }

    def promoMenuTypeInsets(menuType: String, recipes: List[Recipe]): IO[Unit] = {
      (deleteBeforeInsets(sO.executionDate, menuType) *>
        insertIntoRecipesByWeek(recipes, sO.executionDate, menuType) *>
      insertOffer("Промо на 2", sO.executionDate, menuType, recipes, 2) *>
      insertOffer(" Промо на 4", sO.executionDate, menuType, recipes, 4)). transact(xa)
    }

    def deleteBeforeInsets(date: Date, menuType: String): ConnectionIO[Int] = {
      sql"delete from offers where execution_date = $date and menu_type = $menuType".update.run *>
      sql"delete from recipes_by_weeks where execution_date = $date and menu_type = $menuType".update.run
    }

    def insertIntoRecipesByWeek(recipes: List[Recipe], date: Date , menuType: String): ConnectionIO[List[Int]] = {
      recipes.traverse{ r =>
        sql"insert into recipes_by_weeks (recipe_id, execution_date, menu_type) values (${r.id}, $date, $menuType)".update.run
      }
    }

    val menuType = sO.menuType

    if(!validationError(menuType, sO.recipeIds.length)) false
    else {
      val recipes = sO.recipeIds.traverse{ id =>
        sql"select * from recipes where id = $id".query[Recipe].unique
      }
      recipes.map{ recipes =>
        if(menuType == "soup" || menuType == "desert") sideMenuTypeInsets(menuType, recipes).unsafeRunAsyncAndForget()
        else if (menuType == "promo") promoMenuTypeInsets(menuType, recipes).unsafeRunAsyncAndForget()
        else primeMenuTypeInsets(menuType, recipes).unsafeRunAsyncAndForget()
      }.transact(xa).unsafeRunSync()
      /**
       *  Returning true for a version with out unsafeRun in Controller
       * */
      true
    }
  }

  def getAllRecipesOnThisWeek(date: Date): List[Recipe] = {
    (for {
      ids <- sql"select recipe_id from recipes_by_weeks where execution_date = $date".query[Int].to[List]
      recipes <- ids.traverse{ id =>
        sql"select * from recipes where id = $id".query[Recipe].unique
      }
    } yield recipes).transact(xa).unsafeRunSync()
  }

}

case class Offer(id: Option[Int], name: String, price: Int, executionDate: Option[Date], menuType: String)

case class Recipe(id: Option[Int], name: String, menuType: String, edited: Boolean)

case class OfferRecipes(offerId: Int, recipesId: Int, quantity: Int)

case class SettingOffer(menuType: String, executionDate: Date,  recipeIds: List[Int])
case class EditOffer(ids: List[Int], names: List[String], prices: List[Int], executionDate: Date, menuType: String)
case class PassingDate(date: Date)
