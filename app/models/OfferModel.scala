package models

import java.util.Date

import cats.effect.IO
import doobie._
import doobie.implicits._
import cats.implicits._
import javax.inject.{Inject, Singleton}
import scala.reflect.io.File

@Singleton
class OfferModel @Inject()(dS: DoobieStore) {
  protected val xa: DataSourceTransactor[IO] = dS.getXa()
}

case class Offer(id: Int, name: String, price: Int, executionDate: Option[Date], menuType: String)

case class OfferResepies(offerId: Int, resepisId: Int, quantity: Int)

case class OfferForCreate(name: String, file: File)
case class OfferPreferences(names: List[String], prices: List[Int], menuType: String)
