package models

import java.text.SimpleDateFormat
import cats.effect.IO
import doobie._
import doobie.implicits._
import cats.implicits._
import java.util.{Calendar, Date}
import javax.inject._
import services.SimbaAlias._

@Singleton
class OrderModel @Inject()(dS: DoobieStore) {
  private type MenuPrice = Int
  private type MenuTitle = String
  private type Minutes = Int
  protected val xa: DataSourceTransactor[IO] = dS.getXa()

  def getAllTableRows: Map[Date, List[OrderForDisplay]] = {
    def orderToOrderForDisplay(o: Order): OrderForDisplay = {
      def getCustomerById(id: ID): Customer = {
        sql"select * from customers where id = $id".query[Customer].to[List].transact(xa).unsafeRunSync().head
      }

      def getAddressById(id: ID): Address = {
        sql"select * from addresses where id = $id".query[Address].to[List].transact(xa).unsafeRunSync().head
      }

      OrderForDisplay(o.id, getCustomerById(o.customerId), getAddressById(o.addressId),
        o.orderDay, o.deliveryDay, convertMinutesToString(o.deliverFrom), convertMinutesToString(o.deliverTo),
        getAllOfferIdByOrderId(o.id.head).map(_.name).mkString(", "),
        o.total,
        o.payment,
        o.offlineDelivery, o.deliveryOnMonday,
        o.paid, o.delivered,
        o.note)
    }
    sql"select * from orders"
      .query[Order]
      .to[List]
      .transact(xa)
      .map(l => l.map(orderToOrderForDisplay).groupBy(_.deliveryDay))
      .unsafeRunSync
  }


  def insert(o: OrderForEditAndCreate): Boolean = {
      (for {
        _ <-
          sql"""insert into orders (customer_id, address_id,
                                    order_day, delivery_day,
                                    deliver_from, deliver_to,
                                    total,
                                    payment,
                                    offline_delivery, delivery_on_monday,
                                    paid, delivered, note)
                                     values (${o.customerId}, ${o.addressId},
                                              ${o.orderDay}, ${o.deliveryDay},
                                              ${convertStringToMinutes(o.deliverFrom)}, ${convertStringToMinutes(o.deliverTo)},
                                              ${o.total},
                                              ${o.payment},
                                              ${o.offlineDelivery}, ${o.offlineDelivery},
                                              ${o.paid}, ${o.delivered},
                                              ${o.note}) """.update.run
        id <- sql"select LAST_INSERT_ID()".query[Int].unique
        o <- sql"select * from orders where id = $id".query[Order].unique
      } yield o)
        .map{ order =>
          val recipesIdsAndQuantity = getAllOffersRecipes(o.inOrder).map(o => (o.recipesId, o.quantity))
          insertOrderRecipes(order.id.head, recipesIdsAndQuantity) *> insertOrderOffers(order.id.head, o.inOrder)
      }.transact(xa).unsafeRunAsyncAndForget()
    true
  }

  def findById(id:Int): OrderForEditAndCreate = {
    def orderToOrderForEditAndCreate(o: Order): OrderForEditAndCreate = {
      /**
       * Have to use is operation on java.util.Date that was parsed from "yyyy-MM-dd" format
       * otherwise play.api.data.Form will throw [UnsupportedOperationException: null] from java.sql.Date.toInstant
       * However if you parsed java.util.Date form "EEE MMM dd HH:mm:ss zzz yyyy" format or use { new java.util.Date() } it will work
      * */
      def changingDateFormatForPlayForm(date: Date): Date = {
        val formatForFillInForm = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
       formatForFillInForm.parse(formatForFillInForm.format(date))
      }

      OrderForEditAndCreate(o.id, o.customerId, o.addressId,
        changingDateFormatForPlayForm(o.orderDay), changingDateFormatForPlayForm(o.deliveryDay),
        convertMinutesToString(o.deliverFrom), convertMinutesToString(o.deliverTo),
        getAllOfferIdByOrderId(o.id.head).map(_.id.head),
        o.total,
        o.payment,
        o.offlineDelivery, o.deliveryOnMonday,
        o.paid, o.delivered,
        o.note)
    }
  sql"select * from orders where id = $id"
     .query[Order]
     .unique
     .transact(xa)
     .map(orderToOrderForEditAndCreate)
     .unsafeRunSync()
  }

  def edit(o: OrderForEditAndCreate): Boolean = {
    val recipesIdsAndQuantity = getAllOffersRecipes(o.inOrder).map(o => (o.recipesId, o.quantity))
    (sql"delete from order_offers where order_id = ${o.id.head}".update.run *>
      insertOrderOffers(o.id.head, o.inOrder) *>
      sql"delete from order_recipes where order_id = ${o.id.head}".update.run *>
      insertOrderRecipes(o.id.head, recipesIdsAndQuantity)
      ).transact(xa).unsafeRunSync()
    true
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
      OrderMenuItem(o.name, o.id.head, o.price)
    }

    def getAllMenuToolsForAddingOrder(offers: List[Offer]): List[OrderMenu] = {
      offers.groupBy(_.menuType).map { t =>
        OrderMenu(translateMenuType(t._1), t._2.map(offerToMenuItem))
      }.toList
    }

    getAllMenuToolsForAddingOrder(getAllOffersOnThisWeek)
  }

  def getInOrderToTextWithCostMap: Map[String, (ID, MenuTitle, MenuPrice)] = { // TODO aks supervisor's opinion maybe there is better way to make it done
    getAllOffersOnThisWeek.groupBy(_.id.head.toString).map{t =>
      t._1 -> t._2.map(offer => (t._1.toInt, offer.name, offer.price)).head
    }
  }

  private def getAllOffersOnThisWeek: List[Offer] = {
    val c = Calendar.getInstance
    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    val sundayOnCurrentWeek = c.getTime
    sql"select * from offers where execution_date = $sundayOnCurrentWeek"
      .query[Offer]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
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

  private def insertOrderRecipes(orderId: Int, recipisIdsQuantity: List[(Int, Int)]): ConnectionIO[Int] = {
    recipisIdsQuantity.traverse { rIdQ =>
      sql"insert into order_recipes (order_id, recipe_id, quantity) value ($orderId, ${rIdQ._1}, ${rIdQ._2})".update.run
    }.map(_.sum)
  }

  private def getAllOffersRecipes(ids: List[Int]): List[OfferRecipes] = {
    ids.traverse { offerId =>
      sql"select * from offer_recipes where offer_id = $offerId".query[OfferRecipes].to[List]
    }.transact(xa).unsafeRunSync().flatten
  }

}


case class Order(id: Option[ID],
                 customerId: ID,
                 addressId: ID,
                 orderDay: Date, deliveryDay: Date,
                 deliverFrom: Int, deliverTo: Int,
                 total: Int,
                 payment: String,
                 offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                 paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderForDisplay(id: Option[ID],
                           customer: Customer,
                           address: Address,
                           orderDay: Date, deliveryDay: Date,
                           deliverFrom: String, deliverTo: String,
                           inOrder: String,
                           total: Int,
                           payment: String,
                           offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                           paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderForEditAndCreate(id: Option[ID],
                                 customerId: ID,
                                 addressId: ID,
                                 orderDay: Date, deliveryDay: Date,
                                 deliverFrom: String, deliverTo: String,
                                 inOrder: List[Int],
                                 total: Int,
                                 payment: String,
                                 offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                                 paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderMenuItem(titleOnDisplay: String, value: Int, cost: Int)

case class OrderMenu(titleOnDisplay: String, menuItems: List[OrderMenuItem])

case class OrderResepies(orderId: ID, resepisId: ID, quantity: Int)

case class OrderOffer(orderId: ID, offerId: ID)
