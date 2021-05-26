package models

import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}
import javax.inject._
import com.itextpdf.text.{Document, Font, Paragraph}
import com.itextpdf.text.pdf.{BaseFont, PdfPCell, PdfPTable, PdfWriter}
import dao.Dao
import org.joda.time.DateTime
import services.SimbaAlias._
import services.SimbaHTMLHelper._
import zio.{Task, ZIO}

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


  def getAllOrdersWhere(date: DateTime): Task[Seq[OrderForDisplay]] = {
    dao.getAllOrdersWhere(date.withDayOfWeek(7).toDate).flatMap { orders =>
      ZIO.collectAllPar(orders
        .map(orderToOrderForDisplay)
        .toList)
    }
  }

  def insert(o: OrderInput): Task[Unit] = {
    dao.insertOrder(o)
  }

  def findBy(id: ID): Task[OrderInput] = {
    for {
      order <- dao.getOrderBy(id)
      offers <- getAllOfferIdByOrderId(order.id.head)
    } yield OrderInput(order, offers.flatMap(t => List.fill(t._2)(t._1.id.head)))
  }

  def edit(o: OrderInput): Task[Unit] = {
    dao.editOrder(o)
  }

  def getMenusToolsForAddingToOrder: Task[Seq[OrderMenu]] = {
    def offerToMenuItem(o: Offer): OrderMenuItem = {
      OrderMenuItem(o.name, o.id.head, o.price)
    }

    def getAllMenuToolsForAddingOrder(offers: Seq[Offer]): Seq[OrderMenu] = {
      offers.groupBy(_.menuType).map { t =>
        OrderMenu(convertMenuTypeToString(t._1), t._2.map(offerToMenuItem))
      }.toList
    }
    getAllOffersOnThisWeek.map(getAllMenuToolsForAddingOrder)
  }

  def getInOrderToTextWithCostMap: Task[Map[String, (ID, MenuTitle, MenuPrice)]] = {
    getAllOffersOnThisWeek.map(os => {
      os.groupBy(_.id.head.toString).map { t =>
        t._1 -> t._2.map(offer => (t._1.toInt, offer.name, offer.price)).head
      }
    })
  }

  def getPayments: List[String] = payments

//  def generateCourierStickers(date: Date): IO[Array[Byte]] = {
//    def orderToCourierSticker(o: Order): CourierSticker = {
//      (for {
//        address <- dao.getAddressBy(o.addressId)
//        offers <- dao.getAllOfferIdByOrder(o.id.head)
//      } yield CourierSticker(address,
//        convertMinutesToString(o.deliverFrom), convertMinutesToString(o.deliverTo),
//        inOrder = inOrderToString(offers, "+")))
//    }
//
//    def getParagraphWithFont(text: String, font: Font): Paragraph = {
//      new Paragraph(text, font)
//    }
//
//    def getStingFromOptional(option: Option[String], needComa: Boolean = true): String = option match {
//      case Some(value) => value + (if (needComa) "," else "")
//      case None => ""
//    }
//
//    dao.getAllOrdersWhere(date)
//      .map(_.map(orderToCourierSticker).toArray)
//      .map { ss =>
//        val document = new Document()
//        val byteArrayOutputStream = new ByteArrayOutputStream()
//        val bf = BaseFont.createFont("resourses/arialun.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
//        val mainFont = new Font(bf, 14)
//        val table = new PdfPTable(3)
//        val writer = PdfWriter.getInstance(document, byteArrayOutputStream)
//
//        document.open()
//        table.setTotalWidth(Array(198.425f, 198.425f, 198.425f))
//        table.setLockedWidth(true)
//        val canvas = writer.getDirectContent
//        table.writeSelectedRows(0, 0, document.left() + table.getTotalWidth, document.top(), canvas)
//
//        ss.foreach { sticker =>
//          val cell = new PdfPCell(getParagraphWithFont(
//            s"""
//           ${if (!sticker.address.city.contains("Київ")) sticker.address.city else ""}
//           ${sticker.address.address}
//           ${getStingFromOptional(sticker.address.entrance)} ${getStingFromOptional(sticker.address.floor)} ${getStingFromOptional(sticker.address.flat, needComa = false)}
//           ${sticker.deliverFrom}-${sticker.deliverTo}
//           ${sticker.inOrder}
//          """, font = mainFont))
//          cell.setFixedHeight(172.913f)
//          table.addCell(cell)
//        }
//        // Adding empty cells
//        (0 to ss.length % 3).foreach(_ => table.addCell(""))
//
//        document.add(table)
//        document.close()
//        byteArrayOutputStream.toByteArray
//      }
//  }

  private def getAllOffersOnThisWeek: Task[Seq[Offer]] = {
    ZIO(DateTime.now().withDayOfWeek(7))
      .map(_.formatted("yyyy-MM-dd"))
      .flatMap(dao.getOffersByDate)
  }

  private def orderToOrderForDisplay(o: Order): Task[OrderForDisplay] = {
    for {
      customer <- dao.getCustomerBy(o.customerId)
      address <- dao.getAddressBy(o.addressId)
      inviter <- if (o.inviterId.isDefined) dao.getCustomerBy(o.inviterId.head).map(c => Option(c)) else Task(None)
      offers <- getAllOfferIdByOrderId(o.id.head)
    } yield OrderForDisplay(o, customer, address, inviter, offers)
  }

  private def getAllOfferIdByOrderId(id: ID): Task[List[(Offer, Int)]] = {
    dao.getAllOfferIdByOrder(id)
  }

}

case class Order(id: Option[Int],
                 customerId: Int,
                 addressId: Int,
                 inviterId: Option[Int],
                 orderDay: Date, deliveryDay: Date,
                 deliverFrom: Int, deliverTo: Int,
                 offlineDelivery: Boolean, deliveryOnMonday: Boolean,
                 total: Int,
                 discount: Option[Int],
                 payment: Int,
                 paid: Boolean, delivered: Boolean, note: Option[String])

case class OrderForDisplay(order: Order, customer: Customer, address: Address, inviter: Option[Customer], inOrder: List[(Offer, Int)])

case class OrderInput(order: Order, inOrder: List[Int])

case class CourierSticker(address: Address, deliverFrom: String, deliverTo: String, inOrder: String)

case class OrderMenuItem(titleOnDisplay: String, value: Int, cost: Int)

case class OrderMenu(titleOnDisplay: String, menuItems: Seq[OrderMenuItem])

case class OrderRecipes(orderId: ID, offerId: ID, recipesId: ID, menuForPeople: Int, quantity: Int)
