package models

import java.util.Date

import cats.effect.IO
import doobie._
import doobie.implicits._
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.libs.functional.syntax._

@Singleton
class OfferModel @Inject()(dS: DoobieStore) {
  protected val xa: DataSourceTransactor[IO] = dS.getXa()
  private implicit val recipesWriter: Writes[Recipe] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "menuType").write[String] and
      (JsPath \ "edited").write[Boolean]
  )(unlift(Recipe.unapply))
  def getOfferPreferencesByMenuTupe(menuType: String): OfferPreferences = {
    val offers = sql"select * from offers where execution_date is null and  menu_type = $menuType"
      .query[Offer]
      .to[List]
      .transact(xa)
      .unsafeRunSync()

    OfferPreferences(Option(offers.map(_.id.head)), offers.map(_.name), offers.map(_.price), menuType)
  }

  def setOfferPreferences(offerPreferences: OfferPreferences): Boolean = {
    ???
  }

  def getRecipesByName(name: String): JsValue = {
    val recipes: List[Recipe] = sql"select * from recipes where name like $name".query[Recipe].to[List].transact(xa).unsafeRunSync()
    Json.toJson(recipes)
  }

  def setOffer(offerForCreate: OfferForCreate): Boolean = {
    ???
  }

}

case class Offer(id: Option[Int], name: String, price: Int, executionDate: Option[Date], menuType: String)

case class Recipe(id: Option[Int], name: String, menuType: String, edited: Boolean)

case class OfferResepies(offerId: Int, resepisId: Int, quantity: Int)

case class OfferForCreate(menuType: String, recipeIds: List[Int])
case class OfferPreferences(ids: Option[List[Int]], names: List[String],  prices: List[Int], menuType: String)
