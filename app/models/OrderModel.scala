package models
import cats.effect.IO
import doobie._
import doobie.implicits._
import java.util.Date
import java.time.LocalTime
import javax.inject._

@Singleton
class OrderModel @Inject()(dS: DoobieStore) {
  protected val xa: DataSourceTransactor[IO] = dS.getXa()
  private var classicMenu: Map[String, Recipe] = Map()
  private var liteMenu: Map[String, Recipe] = Map()
  private var blackestMenu: Map[String, Recipe] = Map()
//  private var soup: Recipe = ???
//  private var desert: Recipe = ???

  def getAllTableRows: Map[Date, List[Order]] = { // TODO fix bug with query: I can't cast select in Order
//        sql"select * from orders"
//          .query[Order]
//          .to[List]
//          .transact(xa)
//          .unsafeRunSync.groupBy(_.deliveryDay)
    Map()
  }
}

case class Order(id: Option[Int],
                 customerId: Int,
                 orderDay: Date, deliveryDay: Date,
                 deliverFrom: LocalTime, deliverTo: LocalTime,
                 total: Int,
                 paid: Boolean, delivered: Boolean, note: Option[String])

case class Recipe(id: Int, name:String, quantity:Int)

case class PlayOrderForDisplay(id: Option[Int],
                               customer: Customer,
                               orderDay: Date, deliveryDay: Date,
                               deliverFrom: LocalTime, deliverTo: LocalTime,
                               inOrder:String,
                               total: Int,
                               paid: Boolean, delivered: Boolean, note: Option[String])

case class PlayOrderForEditAndCreate(id: Option[Int],
                               customerID: Int,
                               orderDay: Date, deliveryDay: Date,
                               deliverFrom: LocalTime, deliverTo: LocalTime,
                               inOrder:String,
                               total: Int,
                               paid: Boolean, delivered: Boolean, note: Option[String])

