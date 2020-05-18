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

  def getAllTableRows: Seq[Order] = { // TODO fix bug with query: I can't cast select in Order
//        sql"select * from orders"
//          .query[Order]
//          .to[List]
//          .transact(xa)
//          .unsafeRunSync
      List()
  }
}

case class Order(id: Option[Int],
                 customerId: Int,
                 orderDay: Date, deliveryDay: Date,
                 deliverFrom: LocalTime, deliverTo: LocalTime,
                 total: Int,
                 paid: Boolean, delivered: Boolean, note: Option[String])
case class Recipe(id: Int, name:String, quantity:Int)

case class PlayOrder(id: Option[Int],
                     customerId: Int,
                     orderDay: Date, deliveryDay: Date,
                     deliverFrom: LocalTime, deliverTo: LocalTime,
                     total: Int,
                     paid: Boolean, delivered: Boolean, note: Option[String],
                     classic1:Boolean, classic2: Boolean, classic3:Boolean, classic4:Boolean, classic5:Boolean)

