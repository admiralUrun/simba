package models

import java.util.Date
import javax.inject.{Inject, Singleton}
import zio.Task
import org.joda.time.{DateTime, DateTimeConstants}
import dao.Dao
import services.SimbaAlias._
import services.SimbaHTMLHelper.addressToString

@Singleton
class CustomerModel @Inject()(dao: Dao) {
  private val discountForInviteNewCustomers = 100

  def getAllCustomerTableRows: Task[Seq[Seq[Customer]]] =
    dao.getAllCustomers
      .map(_.grouped(50).toSeq)

  def getAllCustomerTableRowsLike(search: String): Task[Seq[Seq[Customer]]] = {
    dao.getAllCustomerTableRowsLike(search)
      .map(cs => cs.grouped(50).toSeq)
  }

  def insert(c: CustomerInput): Task[ID] = {
    dao.insertCustomer(c)
  }

  def findBy(id: ID): Task[CustomerInput] = {
    for {
      c <- dao.getCustomerBy(id)
      addresses <- dao.getAllCustomersAddresses(c.id.head)
    } yield CustomerInput(
      c.id, c.firstName, c.lastName,
      c.phone, c.phoneNote,
      c.phone2, c.phoneNote2,
      c.instagram, c.preferences, c.notes,
      addresses.toList)
  }

  def editCustomer(c: CustomerInput): Task[Unit] = {
    dao.editCustomer(c)
  }

  def getDataForJsonToDisplayInOrderBy(id: ID): Task[CustomerAddressesForJson] = {
    for {
      customer <- dao.getCustomerBy(id)
      addresses <- dao.getAllCustomersAddresses(id)
      discount <- dao.getDiscountFormCustomerBy(customer.id.head, getLastSundayAndMonday)
    } yield CustomerAddressesForJson(customer, addresses, discount * discountForInviteNewCustomers)
  }

  def getInviterForJsonToDisplayInOrderBy(id: ID): Task[Customer] = {
    for {
      customer <- dao.getCustomerBy(id)
    } yield customer
  }

  def getDataForJsonToDisplayInOrder(search: String): Task[Seq[CustomerAddressesForJson]] = {
    def customerToJsonToDisplayInOrder(c: Customer): Task[CustomerAddressesForJson] = {
      for {
        addresses <- dao.getAllCustomersAddresses(c.id.head)
        discount <- dao.getDiscountFormCustomerBy(c.id.head, getLastSundayAndMonday)
      } yield CustomerAddressesForJson(c, addresses, discount * discountForInviteNewCustomers)
    }

    dao.getAllCustomerTableRowsLike(search).flatMap{ cs =>
      Task.foreach(cs)(customerToJsonToDisplayInOrder)
    }
  }

  def getInviterForJsonToDisplayInOrder(search: String): Task[Seq[Customer]] = dao.getAllCustomerTableRowsLike(search)

  def getStreets(search: String): Task[Seq[String]] = dao.getStreets(search)

  private def getLastSundayAndMonday: (Date, Date) = {
    val today = DateTime.now
    val sameDayLastWeek = today.minusWeeks(1)
    val lastMonday = today.withDayOfWeek(DateTimeConstants.MONDAY).toDate
    val lastSunday = sameDayLastWeek.withDayOfWeek(DateTimeConstants.SUNDAY).toDate
    (lastSunday, lastMonday)
  }
}

case class Customer(id: Option[Int],
                    firstName: String, lastName: Option[String],
                    phone: String, phoneNote: Option[String],
                    phone2: Option[String], phoneNote2: Option[String],
                    instagram: Option[String],
                    preferences: Option[String], notes: Option[String])

case class CustomerInput(id: Option[Int],
                         firstName: String, lastName: Option[String],
                         phone: String, phoneNote: Option[String],
                         phone2: Option[String], phoneNote2: Option[String],
                         instagram: Option[String],
                         preferences: Option[String], notes: Option[String],
                         addresses: List[Address])

case class Address(id: Option[Int], customerId: Option[Int],
                   city: String,
                   residentialComplex: Option[String],
                   address: String,
                   entrance: Option[String],
                   floor: Option[String],
                   flat: Option[String],
                   notesForCourier: Option[String])

case class CustomerAddressesForJson(customer: Customer, addresses: Seq[Address], discount: Int)
