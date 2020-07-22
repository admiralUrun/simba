package models

import java.util.Date
import cats.effect.IO
import doobie._
import doobie.implicits._
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.SimbaHTMLHelper.translateMenuType

@Singleton
class OfferModel @Inject()(dS: DoobieStore) {
  protected val xa: DataSourceTransactor[IO] = dS.getXa()
  private implicit val recipesWriter: Writes[Recipe] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "menuType").write[String] and
      (JsPath \ "edited").write[Boolean]
  )(unlift(Recipe.unapply))
  def getOfferPreferencesByMenuType(menuType: String): EditOffer = {
    val offers = sql"select * from offers where execution_date is null and  menu_type = $menuType"
      .query[Offer]
      .to[List]
      .transact(xa)
      .unsafeRunSync()

    EditOffer(offers.map(_.id.head), offers.map(_.name), offers.map(_.price), menuType)
  }

  def setOfferPreferences(editOffer: EditOffer): Boolean = {
    if(editOffer.ids.length != editOffer.prices.length || editOffer.ids.length != editOffer.names.length) false
    else {
      editOffer.ids.zip(editOffer.names.zip(editOffer.prices)).traverse { case (id, (name, price)) =>
        sql"update offers set name =$name, price= $price where id= $id".update.run
      }.transact(xa).unsafeRunAsyncAndForget()
      true
    }
  }

  def getRecipesByName(name: String): JsValue = {
    val recipes: List[Recipe] = sql"select * from recipes where name like $name".query[Recipe].to[List].transact(xa).unsafeRunSync()
    Json.toJson(recipes)
  }

  def setOffer(sO: SettingOffer): Boolean = {
    def validationError(menuType: String, howManyIds: Int): Boolean = Map(
        "promo" -> (howManyIds == 3),
        "soup" -> (howManyIds >= 1),
        "desert" -> (howManyIds >= 1),
        "classic" -> (howManyIds == 5),
        "lite" -> (howManyIds == 5),
        "breakfast" -> (howManyIds == 5)
      )(menuType)

    def insertOffer(name: String, menuType: String, recipes: List[Recipe], quantityOfRecipes: Int): IO[Unit] = {
      val offerId = {
        sql"insert into offers (name, price, execution_date, menu_type) values ($name, 0, null, $menuType)".update.run *>
        sql"select LAST_INSERT_ID()".query[Int].unique
      }
      val effect = offerId.map{ id =>
        recipes.traverse( r =>
          sql"insert into offer_recipes (offer_id, recipe_id, quantity) values ($id, ${r.id}, $quantityOfRecipes)".update.run
        ).transact(xa).unsafeRunAsyncAndForget()
      }
      effect.transact(xa)
    }

    def primeMenuTypeInsets(menuType: String, recipes: List[Recipe]): IO[List[Unit]] = {
      insertOffer(s"5 на 4 ${translateMenuType(menuType)}", menuType, recipes, 4) *>
        insertOffer(s"5 на 2 ${translateMenuType(menuType)}", menuType, recipes, 2) *>
        insertOffer(s"3 на 4 ${translateMenuType(menuType)}", menuType, recipes.take(3), 4) *>
        insertOffer(s"3 на 2 ${translateMenuType(menuType)}", menuType, recipes.take(3), 2) *>
        recipes.zipWithIndex.traverse { case (r, i) =>
          insertOffer(s"${translateMenuType(menuType)} ${i + 1} на 4", menuType, List(r), 4) *>
          insertOffer(s"${translateMenuType(menuType)} ${i + 1} на 2", menuType, List(r), 2)
        }
    }

    def sideMenuTypeInsets(menuType: String, recipes: List[Recipe]): IO[List[Unit]] = {
      recipes.traverse{ r =>
        insertOffer(s"${r.name}", menuType, List(r), 2)
      }
    }

    def promoMenuTypeInsets(menuType: String, recipes: List[Recipe]): IO[Unit] = {
      insertOffer("Промо на 2", menuType, recipes, 2) *>
      insertOffer(" Промо на 4", menuType, recipes, 4)
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

}

case class Offer(id: Option[Int], name: String, price: Int, executionDate: Option[Date], menuType: String)

case class Recipe(id: Option[Int], name: String, menuType: String, edited: Boolean)

case class OfferResepies(offerId: Int, resepisId: Int, quantity: Int)

case class SettingOffer(menuType: String, recipeIds: List[Int])
case class EditOffer(ids: List[Int], names: List[String], prices: List[Int], menuType: String)
