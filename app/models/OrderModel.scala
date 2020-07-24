package models

import cats.effect.IO
import doobie._
import doobie.implicits._
import cats.implicits._
import java.util.Date
import javax.inject._

@Singleton
class OrderModel @Inject()(dS: DoobieStore) {
  private type MenuPrice = Int
  private type MenuTitle = String
  private type Minutes = Int
  protected val xa: DataSourceTransactor[IO] = dS.getXa()

  def getAllTableRows: Map[Date, List[OrderForDisplay]] = {
    sql"select * from orders"
      .query[Order]
      .to[List]
      .transact(xa)
      .unsafeRunSync.map(orderToOrderForDisplay).groupBy(_.deliveryDay)
  }

  def insert(o: OrderForEditAndCreate): Boolean = {
    def insertOrder(o: OrderForEditAndCreate): Int = {
      (for {
        _ <-
          sql"""insert into orders (customer_id, address_id,
                                    order_day, delivery_day,
                                    deliver_from, deliver_to,
                                    total,
                                    offline_delivery, delivery_on_monday,
                                    paid, delivered, note)
                                     values (${o.customerId}, ${o.addressId},
                                              ${o.orderDay}, ${o.deliveryDay},
                                              ${convertStringToMinutes(o.deliverFrom)}, ${convertStringToMinutes(o.deliverTo)},
                                              ${o.total},
                                              ${o.offlineDelivery}, ${o.offlineDelivery},
                                              ${o.paid}, ${o.delivered},
                                              ${o.note}) """.update.run
        id <- sql"select LAST_INSERT_ID()".query[Int].unique
        o <- sql"select * from orders where id = $id".query[Order].unique
      } yield o).transact(xa).unsafeRunSync().id.head
    }

    val orderId = insertOrder(o)
    val resepisIdsAndQuantity = getAllOffersRecipes(o.inOrder).map(o => (o.resepisId, o.quantity))

    (insertOrderRecipis(orderId, resepisIdsAndQuantity) *> insertOrderOffers(orderId, o.inOrder)).transact(xa).unsafeRunSync() == o.inOrder.length * 2
  }

  def findById(id:Int): OrderForEditAndCreate = {
    val order = sql"select * from orders where id = $id".query[Order].to[List].transact(xa).unsafeRunSync().head
    orderToOrderForEditAndCreate(order)
  }

  def edit(o: OrderForEditAndCreate): Boolean = {
    val resepisIdsAndQuantity = getAllOffersRecipes(o.inOrder).map(o => (o.resepisId, o.quantity))
    (sql"delete from order_recipes where order_id = ${o.id.head}".update.run *>
      resepisIdsAndQuantity.traverse { idAndQuantity =>
        sql"insert into order_recipes (order_id, recipe_id, quantity) values (${o.id.head}, ${idAndQuantity._1}, ${idAndQuantity._2})".update.run
      }
      ).transact(xa).unsafeRunSync().length >= o.inOrder.length
  }

  def getMenusToolsForAddingToOrder: List[OrderMenu] = {
    def translateMenuType(string: String): String = {
      val translator = Map(
        "classic" -> "Класичне",
        "lite" -> "Лайт",
        "breakfast" -> "Сніданок",
        "soup" -> "Суп",
        "desert" -> "Десерт"
      )
      translator.getOrElse(string, throw UninitializedFieldError(s"Translation Error, can't translate $string"))
    }

    def offerToMenuItem(o: Offer): OrderMenuItem = {
      OrderMenuItem(o.name, o.id.toString, o.price)
    }

    def getAllMenuToolsForAddingOrder(offers: List[Offer]): List[OrderMenu] = {
      offers.groupBy(_.menuType).map { t =>
        OrderMenu(translateMenuType(t._1), t._2.map(offerToMenuItem))
      }.toList
    }

    getAllMenuToolsForAddingOrder(getAllOffersOnThisWeek)
  }

  def getInOrderToTextWithCostMap: Map[String, (MenuTitle, MenuPrice)] = { // TODO aks supervisor's opinion maybe there is better way to make it done
    getAllOffersOnThisWeek.groupBy(_.id.toString).map{t =>
      t._1 -> t._2.map(offer => (offer.name, offer.price)).head
    }
  }

  private def getAllOffersOnThisWeek: List[Offer] = {
    sql"select * from offers where execution_date is null"
      .query[Offer]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
  }

  private def orderToOrderForDisplay(o: Order): OrderForDisplay = {
    def getCustomerById(id: Int): Customer = {
      sql"select * from customers where id = $id".query[Customer].to[List].transact(xa).unsafeRunSync().head
    }

    def getAddressById(id: Int): Address = {
      sql"select * from addresses where id = $id".query[Address].to[List].transact(xa).unsafeRunSync().head
    }

    OrderForDisplay(o.id, getCustomerById(o.customerId), getAddressById(o.addressId),
      o.orderDay, o.deliveryDay, convertMinutesToString(o.deliverFrom), convertMinutesToString(o.deliverTo),
      getAllOfferIdByOrderId(o.id.head).map(_.name).mkString(", "),
      o.total,
      o.offlineDelivery, o.deliveryOnMonday,
      o.paid, o.delivered,
      o.note)
  }

  private def orderToOrderForEditAndCreate(o: Order): OrderForEditAndCreate = {
    OrderForEditAndCreate(o.id, o.customerId, o.addressId,
      o.orderDay, o.deliveryDay, convertMinutesToString(o.deliverFrom), convertMinutesToString(o.deliverTo),
      getAllOfferIdByOrderId(o.id.head).map(_.id.head),
      o.total,
      o.offlineDelivery, o.deliveryOnMonday,
      o.paid, o.delivered,
      o.note)
  }

  private def convertStringToMinutes(timeInput: String): Minutes = { // Maybe throw exception if String didn't split by ':'
    val timeArray = timeInput.split(':')
    timeArray(0).toInt * 60 + timeArray(1).toInt
  }

  private def convertMinutesToString(m: Minutes): String = {
    def replaceWithDoubleZero(int: Int): String = if(int == 0) "00" else int.toString
    replaceWithDoubleZero(m / 60) + ":" + replaceWithDoubleZero(m % 60)
  }

  private def insertOrderOffers(orderId: Int, offersInOrder: List[Int]): ConnectionIO[Int] = {
    offersInOrder.traverse { offerId =>
      sql"insert into order_offers (order_id, offer_id) values ($orderId, $offerId)".update.run
    }.map(_.sum)
  }

  private def getAllOfferIdByOrderId(id: Int): List[Offer] = {
    sql"select * from order_offers where order_id = $id".query[OrderOffer].to[List].transact(xa).unsafeRunSync().traverse { orderOffer =>
      sql"select * from offers where id = ${orderOffer.offerId}".query[Offer].to[List]
    }.transact(xa).unsafeRunSync().flatten
  }

  private def insertOrderRecipis(orderId: Int, recipisIdsQuantity: List[(Int, Int)]): ConnectionIO[Int] = {
    recipisIdsQuantity.traverse { rIdQ =>
      sql"insert into order_recipes (order_id, recipe_id, quantity) value ($orderId, ${rIdQ._1}, ${rIdQ._2})".update.run
    }.map(_.sum)
  }

  private def getAllOffersRecipes(ids: List[Int]): List[OfferResepies] = {
    ids.traverse { offerId =>
      sql"select * from offer_recipes where offer_id = $offerId".query[OfferResepies].to[List]
    }.transact(xa).unsafeRunSync().flatten
  }

}


case class Order(id: Option[Int],
                 customerId: Int,
                 addressId: Int,
                 orderDay: Date, deliveryDay: Date,
                 deliverFrom: Int, deliverTo: Int,
                 total: Int,
                 offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                 paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderForDisplay(id: Option[Int],
                           customer: Customer,
                           address: Address,
                           orderDay: Date, deliveryDay: Date,
                           deliverFrom: String, deliverTo: String,
                           inOrder: String,
                           total: Int,
                           offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                           paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderForEditAndCreate(id: Option[Int],
                                 customerId: Int,
                                 addressId: Int,
                                 orderDay: Date, deliveryDay: Date,
                                 deliverFrom: String, deliverTo: String,
                                 inOrder: List[Int],
                                 total: Int,
                                 offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                                 paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderMenuItem(titleOnDisplay: String, value: String, cost: Int)

case class OrderMenu(titleOnDisplay: String, menuItems: List[OrderMenuItem])

case class OrderResepies(orderId: Int, resepisId: Int, quantity: Int)

case class OrderOffer(orderId: Int, offerId: Int)
