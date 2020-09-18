package models

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}
import dao.Dao
import cats.effect.IO
import javax.inject._
import services.SimbaAlias._
import services.SimbaHTMLHelper._

@Singleton
class OrderModel @Inject()(dao: Dao) {
  private type MenuPrice = Int
  private type MenuTitle = String
  private type Minutes = Int

  private val paymentToInt = Map(
    "Готівка" -> 1,
    "Карткою" -> 2,
    "Бартер" -> 3)
  private val intToPayment = Map(
    1 -> "Готівка",
    2 -> "Карткою",
    3 -> "Бартер")
  private val payments = List("Готівка", "Карткою", "Бартер")

  def getAllTableRows: Map[Date, Seq[OrderForDisplay]] = {
    def orderToOrderForDisplay(o: Order): OrderForDisplay = {
      (for {
        customer <- dao.getCustomerBy(o.customerId)
        address <- dao.getAddressBy(o.addressId)
        inviter <- if (o.inviterId.isDefined) dao.getCustomerBy(o.inviterId.head).map(c => Option(c)) else IO(None)
      } yield OrderForDisplay(o.id, customer, address, inviter,
        o.orderDay, o.deliveryDay, convertMinutesToString(o.deliverFrom), convertMinutesToString(o.deliverTo),
        getAllOfferIdByOrderId(o.id.head).map(t => t._1.name + s"(${t._2})").mkString(", "),
        o.total, o.discount,
        intToPayment(o.payment),
        o.offlineDelivery, o.deliveryOnMonday,
        o.paid, o.delivered,
        o.note)).unsafeRunSync()
    }

    dao.getAllOrders // TODO maybe get it all under one .unsafeRunSync
      .map(_.map(orderToOrderForDisplay).groupBy(_.deliveryDay))
      .unsafeRunSync
  }

  def insert(o: OrderInput): Boolean = {
    dao.insertOrder(o, convertStringToMinutes, convertPaymentToInt)
      .redeemWith(_ => IO(false), _ => IO(true))
      .unsafeRunSync()
  }

  def findBy(id: ID): OrderInput = {
    def orderToOrderForEditAndCreate(o: Order): OrderInput = {
      /**
       * Have to use is operation on java.util.Date that was parsed from "yyyy-MM-dd" format
       * otherwise play.api.data.Form will throw [UnsupportedOperationException: null] from java.sql.Date.toInstant
       * However if you parsed java.util.Date form "EEE MMM dd HH:mm:ss zzz yyyy" format or use { new java.util.Date() } it will work
       **/
      def changingDateFormatForPlayForm(date: Date): Date = {
        val formatForFillInForm = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
        formatForFillInForm.parse(formatForFillInForm.format(date))
      }

      OrderInput(o.id, o.customerId, o.addressId, o.inviterId,
        changingDateFormatForPlayForm(o.orderDay), changingDateFormatForPlayForm(o.deliveryDay),
        convertMinutesToString(o.deliverFrom), convertMinutesToString(o.deliverTo),
        getAllOfferIdByOrderId(o.id.head).flatMap(t => List.fill(t._2)(t._1.id.head)),
        o.total, o.discount,
        intToPayment(o.payment),
        o.offlineDelivery, o.deliveryOnMonday,
        o.paid, o.delivered,
        o.note)
    }

    dao.getOrderBy(id)
      .map(orderToOrderForEditAndCreate)
      .unsafeRunSync()
  }

  def edit(id: ID, o: OrderInput): Boolean = {
    dao.editOrder(id, o, convertStringToMinutes, convertPaymentToInt)
      .redeemWith(_ => IO(false), _ => IO(true))
      .unsafeRunSync()
  }

  def getMenusToolsForAddingToOrder: Seq[OrderMenu] = {

    def offerToMenuItem(o: Offer): OrderMenuItem = {
      OrderMenuItem(o.name, o.id.head, o.price)
    }

    def getAllMenuToolsForAddingOrder(offers: Seq[Offer]): Seq[OrderMenu] = {
      offers.groupBy(_.menuType).map { t =>
        OrderMenu(convertMenuTypeToString(t._1), t._2.map(offerToMenuItem))
      }.toList
    }

    getAllMenuToolsForAddingOrder(getAllOffersOnThisWeek)
  }

  def getInOrderToTextWithCostMap: Map[String, (ID, MenuTitle, MenuPrice)] = { // TODO aks supervisor's opinion maybe there is better way to make it done
    getAllOffersOnThisWeek.groupBy(_.id.head.toString).map { t =>
      t._1 -> t._2.map(offer => (t._1.toInt, offer.name, offer.price)).head
    }
  }

  def getPayments: List[String] = payments

  private def getAllOffersOnThisWeek: Seq[Offer] = {
    val c = Calendar.getInstance
    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    val sundayOnCurrentWeek = c.getTime
    val format = new SimpleDateFormat("yyyy-MM-dd")
    dao.getOffersByDate(format.format(sundayOnCurrentWeek))
      .unsafeRunSync()
  }

  private def convertStringToMinutes(timeInput: String): Minutes = {
    val timeArray = timeInput.split(':')
    timeArray(0).toInt * 60 + timeArray(1).toInt
  }

  private def convertPaymentToInt(p: String) = paymentToInt(p)

  private def convertMinutesToString(m: Minutes): String = {
    def replaceWithDoubleZero(int: Int): String = if (int == 0) "00" else int.toString

    replaceWithDoubleZero(m / 60) + ":" + replaceWithDoubleZero(m % 60)
  }

  private def getAllOfferIdByOrderId(id: ID): List[(Offer, Int)] = {
    val x  = dao.getAllOfferIdByOrder(id).unsafeRunSync()
    x
  }

}


case class Order(id: Option[ID],
                 customerId: ID,
                 addressId: ID,
                 inviterId: Option[ID],
                 orderDay: Date, deliveryDay: Date,
                 deliverFrom: Int, deliverTo: Int,
                 offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                 total: Int,
                 discount: Option[Int],
                 payment: Int,
                 paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderForDisplay(id: Option[ID],
                           customer: Customer,
                           address: Address,
                           inviter: Option[Customer],
                           orderDay: Date, deliveryDay: Date,
                           deliverFrom: String, deliverTo: String,
                           inOrder: String,
                           total: Int,
                           discount: Option[Int],
                           payment: String,
                           offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                           paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderInput(id: Option[ID],
                      customerId: ID,
                      addressId: ID,
                      inviterId: Option[ID],
                      orderDay: Date, deliveryDay: Date,
                      deliverFrom: String, deliverTo: String,
                      inOrder: List[Int],
                      total: Int,
                      discount: Option[Int],
                      payment: String,
                      offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                      paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderMenuItem(titleOnDisplay: String, value: Int, cost: Int)

case class OrderMenu(titleOnDisplay: String, menuItems: Seq[OrderMenuItem])

case class OrderResepies(orderId: ID, offerId: ID, recipesId: ID, menuForPeople: Int, quantity: Int)
