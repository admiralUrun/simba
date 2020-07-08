package models

import java.util.Date

import cats.effect.IO
import doobie._
import doobie.implicits._
import cats.implicits._
import javax.inject.{Inject, Singleton}
import java.io.File

@Singleton
class OfferModel @Inject()(dS: DoobieStore) {
  protected val xa: DataSourceTransactor[IO] = dS.getXa()

  def getOfferPreferencesByMuneTupe(menuType: String): OfferPreferences = {
    val offers = sql"select * from offers where execution_date is null and  menu_type = $menuType"
      .query[Offer]
      .to[List]
      .transact(xa)
      .unsafeRunSync()

    OfferPreferences(offers.map(_.name), offers.map(_.price), menuType)
  }

  def setOfferPreferences(offerPreferences: OfferPreferences): Boolean = {
    ???
  }

  def setOffer(offerForCreate: OfferForCreate): Boolean = {


    true
  }

}

case class Offer(id: Int, name: String, price: Int, executionDate: Option[Date], menuType: String)

case class OfferResepies(offerId: Int, resepisId: Int, quantity: Int)

case class OfferForCreate(menuType: String, file: File)
case class OfferPreferences(names: List[String], prices: List[Int], menuType: String)
