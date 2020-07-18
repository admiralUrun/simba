package models

import java.util.Date

import cats.effect.IO
import doobie._
import doobie.implicits._
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue

@Singleton
class OfferModel @Inject()(dS: DoobieStore) {
  protected val xa: DataSourceTransactor[IO] = dS.getXa()

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
    ???
  }

  def setOffer(offerForCreate: OfferForCreate): Boolean = {
    ???
  }

}

case class Offer(id: Option[Int], name: String, price: Int, executionDate: Option[Date], menuType: String)

case class OfferResepies(offerId: Int, resepisId: Int, quantity: Int)

case class OfferForCreate(menuType: String, recipeIds: List[Int])
case class OfferPreferences(ids: Option[List[Int]], names: List[String],  prices: List[Int], menuType: String)
