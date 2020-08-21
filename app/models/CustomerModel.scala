package models

import java.util.Date

import org.joda.time.{DateTime, DateTimeConstants}
import dao.Dao
import cats.effect.IO
import javax.inject._
import services.SimbaAlias._
import services.SimbaHTMLHelper.addressToString

@Singleton
class CustomerModel @Inject()(dao: Dao) {
  private val discountForInviteNewCustomers = 100


  def getAllCustomerTableRows: Seq[Customer] = {
    dao.getAllCustomers.unsafeRunSync()
  }

  def getAllCustomerTableRowsLike(search: String): Seq[Customer] = {
    dao.getAllCustomerTableRowsLike(search).unsafeRunSync
  }

  def insert(c: CustomerInput): (Boolean, ID) = {
    dao.insertCustomer(c).redeemWith(_ => IO(false, -1), b => IO(true, b)).unsafeRunSync()
  }

  def findBy(id: ID): CustomerInput = {
    (for {
      c <- dao.getCustomerBy(id)
      addresses <- dao.getAllCustomersAddresses(c.id.head).map(_.map(addressToString))
    } yield CustomerInput(
      c.id, c.firstName, c.lastName,
      c.phone, c.phoneNote,
      c.phone2, c.phoneNote2,
      c.instagram, c.preferences, c.notes,
      addresses.toList)).unsafeRunSync()
  }

  def editCustomer(id: ID, c: CustomerInput): Boolean = {
    dao.editCustomer(id, c).redeemWith(_ => IO(false), _ => IO(true)).unsafeRunSync()
  }

  def getDataForJsonToDisplayInOrderBy(id: ID): IO[CustomerAddressesForJson] = {
    for {
      customer <- dao.getCustomerBy(id)
      addresses <- dao.getAllCustomersAddresses(id)
      discount <- dao.getDiscountFormCustomerBy(customer.id.head, getLastSundayAndMonday)
    } yield CustomerAddressesForJson(customer, addresses, discount * discountForInviteNewCustomers)
  }

  def getInviterForJsonToDisplayInOrderBy(id: ID): IO[Customer] = {
    for {
      customer <- dao.getCustomerBy(id)
    } yield customer
  }

  def getDataForJsonToDisplayInOrder(search: String): IO[Seq[CustomerAddressesForJson]] = {
    IO(getAllCustomerTableRowsLike(search).map { c =>
      (for {
        addresses <- dao.getAllCustomersAddresses(c.id.head)
        discount <- dao.getDiscountFormCustomerBy(c.id.head, getLastSundayAndMonday)
      } yield CustomerAddressesForJson(c, addresses, discount  * discountForInviteNewCustomers)).unsafeRunSync()
    })
  }

  def getInviterForJsonToDisplayInOrder(search: String): Seq[Customer] = {
    dao.getAllCustomerTableRowsLike(search).unsafeRunSync()
  }

  private def getLastSundayAndMonday: (Date, Date) = {
    val today = DateTime.now
    val sameDayLastWeek = today.minusWeeks(1)
    val lastMonday = today.withDayOfWeek(DateTimeConstants.MONDAY).toDate
    val lastSunday = sameDayLastWeek.withDayOfWeek(DateTimeConstants.SUNDAY).toDate
    (lastSunday, lastMonday)
  }
}

case class Customer(id: Option[ID],
                    firstName: String, lastName: Option[String],
                    phone: String, phoneNote: Option[String],
                    phone2: Option[String], phoneNote2: Option[String],
                    instagram: Option[String],
                    preferences: Option[String], notes: Option[String])

case class CustomerInput(id: Option[ID],
                         firstName: String, lastName: Option[String],
                         phone: String, phoneNote: Option[String],
                         phone2: Option[String], phoneNote2: Option[String],
                         instagram: Option[String],
                         preferences: Option[String], notes: Option[String],
                         addresses: List[String])

case class Address(id: Option[ID], customerId: Option[ID],
                   city: String,
                   residentialComplex: Option[String],
                   address: String,
                   entrance: Option[String],
                   floor: Option[String],
                   flat: Option[String],
                   notesForCourier: Option[String])

case class CustomerAddressesForJson(customer: Customer, addresses: Seq[Address], discount: Int)
