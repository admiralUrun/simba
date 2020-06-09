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

  def getAllTableRows: Map[Date, List[Order]] = { // TODO fix bug with query: I can't cast select in Order
    sql"select * from orders"
      .query[Order]
      .to[List]
      .transact(xa)
      .unsafeRunSync.groupBy(_.deliveryDay)
  }

  def insert(o: PlayOrderForEditAndCreate): Boolean = {
    ???
  }

  def edit(o: PlayOrderForEditAndCreate): Boolean = {
    ???
  }

  def getMenusToolsForAddingToOrder: List[OrderMenu] = { // TODO take it from offers
    List(
      OrderMenu("Класичне", List(
        OrderMenuItem("5 на 2 Класичне", "5 on 2(classic)", 1289),
        OrderMenuItem("5 на 4 Класичне", "5 on 4(classic)", 2249),
        OrderMenuItem("3 на 2 Класичне", "3 on 2(classic)", 849),
        OrderMenuItem("3 на 4 Класичне", "3 on 4(classic)", 1489),
        OrderMenuItem("Класичне 1", "1(classic)", 70),
        OrderMenuItem("Класичне 2", "2(classic)", 70),
        OrderMenuItem("Класичне 3", "3(classic)", 70),
        OrderMenuItem("Класичне 4", "4(classic)", 70),
        OrderMenuItem("Класичне 5", "5(classic)", 70))),
      OrderMenu("Лайт", List(
        OrderMenuItem("5 на 2 Лайт", "5 on 2(lite)", 1289),
        OrderMenuItem("5 на 4 Лайт", "5 on 4(lite)", 2249),
        OrderMenuItem("3 на 2 Лайт", "3 on 2(lite)", 849),
        OrderMenuItem("3 на 4 Лайт", "3 on 4(lite)", 1489),
        OrderMenuItem("Лайт 1", "1(lite)", 70),
        OrderMenuItem("Лайт 2", "2(lite)", 70),
        OrderMenuItem("Лайт 3", "3(lite)", 70),
        OrderMenuItem("Лайт 4", "4(lite)", 70),
        OrderMenuItem("Лайт 5", "5(lite)", 70))),
      OrderMenu("Сніданок", List(
        OrderMenuItem("5 на 2 Сніданок", "5 on 2(breakfast)", 849),
        OrderMenuItem("5 на 4 Сніданок", "5 on 4(breakfast)", 1589),
        OrderMenuItem("3 на 2 Сніданок", "3 on 2(breakfast)", 549),
        OrderMenuItem("3 на 4 Сніданок", "3 on 4(breakfast)", 989),
        OrderMenuItem("Сніданок 1", "1(breakfast)", 70),
        OrderMenuItem("Сніданок 2", "2(breakfast)", 70),
        OrderMenuItem("Сніданок 3", "3(breakfast)", 70),
        OrderMenuItem("Сніданок 4", "4(breakfast)", 70),
        OrderMenuItem("Сніданок 5", "5(breakfast)", 70))),
      OrderMenu("Десерт", List(OrderMenuItem("Десерт", "desert", 249))),
      OrderMenu("Суп", List(OrderMenuItem("Суп", "soup", 229))),
    )
  }

  def getInOrderToTextWithCostMap: Map[String, (String, Int)] = {
    Map()
  }
}

case class Order(id: Option[Int],
                 customerId: Int,
                 orderDay: Date, deliveryDay: Date,
                 deliverFrom: Date, deliverTo: Date,
                 total: Int,
                 offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                 paid: Boolean, delivered: Boolean, note: Option[String])

case class Recipe(id: Option[Int], name:String)

case class PlayOrderForDisplay(id: Option[Int],
                               customer: Customer,
                               orderDay: Date, deliveryDay: Date,
                               deliverFrom: LocalTime, deliverTo: LocalTime,
                               inOrder:String,
                               total: Int,
                               offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                               paid: Boolean, delivered: Boolean, note: Option[String])

case class PlayOrderForEditAndCreate(id: Option[Int],
                                     customerID: Int,
                                     orderDay: Date, deliveryDay: Date,
                                     deliverFrom: String, deliverTo: String,
                                     inOrder: String,
                                     total: Int,
                                     offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                                     paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderMenuItem(name: String, value: String, cost: Int)
case class OrderMenu(title: String, menuItems: List[OrderMenuItem])

case class Offer(id: Int, name: String, price: Int, menuType: String)

case class RecipesPrices(first: Int, second: Int, third: Int, fourth: Int, fifth: Int)