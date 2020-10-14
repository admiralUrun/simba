package models

import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}
import javax.inject._
import play.api.Logger
import cats.effect.IO
import cats.implicits._
import com.itextpdf.text.{Document, Font, Paragraph, Element}
import com.itextpdf.text.pdf.{BaseFont, PdfPCell, PdfPTable, PdfWriter}
import dao.Dao
import services.SimbaAlias._
import services.SimbaHTMLHelper._

@Singleton
class OrderModel @Inject()(dao: Dao) {
  private type MenuPrice = Int
  private type MenuTitle = String
  private type Minutes = Int
  private val logger = Logger("OrderModelLogger")

  private val paymentToInt = Map(
    "Готівка" -> 1,
    "Карткою" -> 2,
    "Бартер" -> 3)
  private val intToPayment = Map(
    1 -> "Готівка",
    2 -> "Карткою",
    3 -> "Бартер")
  private val payments = List("Готівка", "Карткою", "Бартер")

  def getAllTableRows: IO[Map[Date, Seq[OrderForDisplay]]] = {
    def orderToOrderForDisplay(o: Order): IO[OrderForDisplay] = {
      for {
        customer <- dao.getCustomerBy(o.customerId)
        address <- dao.getAddressBy(o.addressId)
        inviter <- if (o.inviterId.isDefined) dao.getCustomerBy(o.inviterId.head).map(c => Option(c)) else IO(None)
        offers <- getAllOfferIdByOrderId(o.id.head)
      } yield OrderForDisplay(o.id, customer, address, inviter,
        o.orderDay, o.deliveryDay, convertMinutesToString(o.deliverFrom), convertMinutesToString(o.deliverTo),
        inOrderToString(offers, ", "),
        o.total, o.discount,
        intToPayment(o.payment),
        o.offlineDelivery, o.deliveryOnMonday,
        o.paid, o.delivered,
        o.note)
    }
    dao.getAllOrders.flatMap{ orders =>
      orders.map(orderToOrderForDisplay).sequence.map(_.groupBy(_.deliveryDay))
    }
  }

  def insert(o: OrderInput): Boolean = {
    dao.insertOrder(o, convertStringToMinutes, convertPaymentToInt)
      .redeemWith(t => {
        logger.error("Can't insert given OrderInput", t)
        IO(false)
      }, _ => IO(true))
      .unsafeRunSync()
  }

  def findBy(id: ID): IO[OrderInput] = {
    /**
     * Have to use is operation on java.util.Date that was parsed. Can't get Offers by this dated with "yyyy-MM-dd" format
     * otherwise play.api.data.Form will throw [UnsupportedOperationException: null] from java.sql.Date.toInstant
     * However if you parsed java.util.Date form "EEE MMM dd HH:mm:ss zzz yyyy" format or use { new java.util.Date() } it will work
     **/
    def changingDateFormatForPlayForm(date: Date): Date = {
      val formatForFillInForm = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
      formatForFillInForm.parse(formatForFillInForm.format(date))
    }

    for {
      order <- dao.getOrderBy(id)
      offers <- getAllOfferIdByOrderId(order.id.head)
    } yield OrderInput(order.id, order.customerId, order.addressId, order.inviterId,
      changingDateFormatForPlayForm(order.orderDay), changingDateFormatForPlayForm(order.deliveryDay),
      convertMinutesToString(order.deliverFrom), convertMinutesToString(order.deliverTo),
      offers.flatMap(t =>  List.fill(t._2)(t._1.id.head)),
      order.total, order.discount,
      intToPayment(order.payment),
      order.offlineDelivery, order.deliveryOnMonday,
      order.paid, order.delivered,
      order.note)
  }

  def edit(id: ID, o: OrderInput): Boolean = {
    dao.editOrder(id, o, convertStringToMinutes, convertPaymentToInt)
      .redeemWith(t => {
        logger.error(s"Can't edit order with given ID = $id", t)
        IO(false)
      }, _ => IO(true))
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

  def getInOrderToTextWithCostMap: Map[String, (ID, MenuTitle, MenuPrice)] = {
    getAllOffersOnThisWeek.groupBy(_.id.head.toString).map { t =>
      t._1 -> t._2.map(offer => (t._1.toInt, offer.name, offer.price)).head
    }
  }

  def getPayments: List[String] = payments

  def generateCourierStickers(date: Date): IO[Array[Byte]] = {
    def orderToCourierSticker(o: Order): CourierSticker = {
      (for {
        address <- dao.getAddressBy(o.addressId)
        offers <- dao.getAllOfferIdByOrder(o.id.head)
      } yield CourierSticker(address,
        convertMinutesToString(o.deliverFrom), convertMinutesToString(o.deliverTo),
        inOrder = inOrderToString(offers, "+") )).unsafeRunSync()
    }
    def getParagraphWithFont(text: String, font: Font): Paragraph = {
      new Paragraph(text, font)
    }
    def getStingFromOptional(option: Option[String], needComa: Boolean = true): String = option match {
      case Some(value) => value + (if(needComa) "," else "")
      case None => ""
    }

    dao.getAllOrdersWhere(date)
      .map(_.map(orderToCourierSticker).toArray)
      .map{ ss =>
      val document = new Document()
      val byteArrayOutputStream = new ByteArrayOutputStream()
      val bf = BaseFont.createFont("resourses/arialun.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
      val mainFont = new Font(bf, 14)
      val table = new PdfPTable(3)
      val writer = PdfWriter.getInstance(document, byteArrayOutputStream)

      document.open()
      table.setTotalWidth(Array(198.425f, 198.425f, 198.425f))
      table.setLockedWidth(true)
      val canvas = writer.getDirectContent
      table.writeSelectedRows(0, 0, document.left() + table.getTotalWidth, document.top(), canvas)

      ss.foreach{ sticker =>
        val cell = new PdfPCell(getParagraphWithFont(s"""
           ${if (!sticker.address.city.contains("Київ")) sticker.address.city else ""}
           ${sticker.address.address}
           ${getStingFromOptional(sticker.address.entrance)} ${getStingFromOptional(sticker.address.floor)} ${getStingFromOptional(sticker.address.flat, needComa = false)}
           ${sticker.deliverFrom}-${sticker.deliverTo}
           ${sticker.inOrder}
          """, font = mainFont))
        cell.setFixedHeight(172.913f)
        table.addCell(cell)
      }
      // Adding empty cells
      (0 to ss.length % 3).foreach(_ => table.addCell(""))

      document.add(table)
      document.close()
      byteArrayOutputStream.toByteArray
    }
  }

  private def inOrderToString(inOrder: List[(Offer, Int)], separator: String): String = {
    inOrder.map(t => t._1.name + (if(t._2 > 1) s"(${t._2})" else "")).mkString(separator)
  }

  private def getAllOffersOnThisWeek: Seq[Offer] = {
    val c = Calendar.getInstance
    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    val sundayOnCurrentWeek = c.getTime
    val format = new SimpleDateFormat("yyyy-MM-dd")
    dao.getOffersByDate(format.format(sundayOnCurrentWeek))
      .redeemWith(t => {
        logger.error(s"Can't get Offers by this date ${format.format(sundayOnCurrentWeek)}", t)
        IO(List())
      }, s => IO(s))
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

  private def getAllOfferIdByOrderId(id: ID): IO[List[(Offer, Int)]] = {
    dao.getAllOfferIdByOrder(id)
      .redeemWith(t => {
        logger.error(s"Can't get Offers by Order ID = $id", t)
        IO(List())
      }, s => IO(s))
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

case class CourierSticker(address: Address, deliverFrom: String, deliverTo: String, inOrder: String)

case class OrderMenuItem(titleOnDisplay: String, value: Int, cost: Int)

case class OrderMenu(titleOnDisplay: String, menuItems: Seq[OrderMenuItem])

case class OrderRecipes(orderId: ID, offerId: ID, recipesId: ID, menuForPeople: Int, quantity: Int)
