package models

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}
import Dao.Dao
import javax.inject._
import services.SimbaAlias._

@Singleton
class OrderModel @Inject()(dao: Dao) {
  private type MenuPrice = Int
  private type MenuTitle = String
  private type Minutes = Int

  def getAllTableRows: Map[Date, Seq[OrderForDisplay]] = {
    def orderToOrderForDisplay(o: Order): OrderForDisplay = {
      OrderForDisplay(o.id, dao.getCustomerById(o.customerId).unsafeRunSync(), dao.getAddressById(o.addressId).unsafeRunSync(),
        o.orderDay, o.deliveryDay, convertMinutesToString(o.deliverFrom), convertMinutesToString(o.deliverTo),
        getAllOfferIdByOrderId(o.id.head).map(_.name).mkString(", "),
        o.total,
        o.payment,
        o.offlineDelivery, o.deliveryOnMonday,
        o.paid, o.delivered,
        o.note)
    }

    dao.getAllOrders
      .map(_.map(orderToOrderForDisplay).groupBy(_.deliveryDay))
      .unsafeRunSync
  }

  def insert(o: OrderForEditAndCreate): Boolean = {
    dao.insertOrder(o, convertStringToMinutes).unsafeRunAsyncAndForget()
    true
  }

  def findById(id: ID): OrderForEditAndCreate = {
    def orderToOrderForEditAndCreate(o: Order): OrderForEditAndCreate = {
      /**
       * Have to use is operation on java.util.Date that was parsed from "yyyy-MM-dd" format
       * otherwise play.api.data.Form will throw [UnsupportedOperationException: null] from java.sql.Date.toInstant
       * However if you parsed java.util.Date form "EEE MMM dd HH:mm:ss zzz yyyy" format or use { new java.util.Date() } it will work
       **/
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

    dao.getOrderByID(id)
      .map(orderToOrderForEditAndCreate)
      .unsafeRunSync()
  }

  def edit(id: ID, o: OrderForEditAndCreate): Boolean = {
    dao.editOrder(id, o, convertStringToMinutes).unsafeRunSync()
    true
  }

  def getMenusToolsForAddingToOrder: Seq[OrderMenu] = {
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

    def getAllMenuToolsForAddingOrder(offers: Seq[Offer]): Seq[OrderMenu] = {
      offers.groupBy(_.menuType).map { t =>
        OrderMenu(translateMenuType(t._1), t._2.map(offerToMenuItem))
      }.toList
    }

    getAllMenuToolsForAddingOrder(getAllOffersOnThisWeek)
  }

  def getInOrderToTextWithCostMap: Map[String, (ID, MenuTitle, MenuPrice)] = { // TODO aks supervisor's opinion maybe there is better way to make it done
    getAllOffersOnThisWeek.groupBy(_.id.head.toString).map { t =>
      t._1 -> t._2.map(offer => (t._1.toInt, offer.name, offer.price)).head
    }
  }

  private def getAllOffersOnThisWeek: Seq[Offer] = {
    val c = Calendar.getInstance
    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    val sundayOnCurrentWeek = c.getTime
    dao.getOffersByDate(sundayOnCurrentWeek)
      .unsafeRunSync()
  }

  private def convertStringToMinutes(timeInput: String): Minutes = { // Maybe throw exception if String didn't split by ':'
    val timeArray = timeInput.split(':')
    timeArray(0).toInt * 60 + timeArray(1).toInt
  }

  private def convertMinutesToString(m: Minutes): String = {
    def replaceWithDoubleZero(int: Int): String = if (int == 0) "00" else int.toString

    replaceWithDoubleZero(m / 60) + ":" + replaceWithDoubleZero(m % 60)
  }

  private def getAllOfferIdByOrderId(id: ID): List[Offer] = {
    dao.getAllOfferIdByOrderId(id).unsafeRunSync()
  }

}


case class Order(id: Option[ID],
                 customerId: ID,
                 addressId: ID,
                 orderDay: Date, deliveryDay: Date,
                 deliverFrom: Int, deliverTo: Int,
                 offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                 total: Int,
                 payment: String,
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

case class OrderMenu(titleOnDisplay: String, menuItems: Seq[OrderMenuItem])

case class OrderResepies(orderId: ID, recipesId: ID, quantity: Int)

case class OrderOffer(orderId: ID, offerId: ID)
