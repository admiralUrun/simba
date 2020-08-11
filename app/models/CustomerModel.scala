package models

import Dao.Dao
import cats.effect.IO
import javax.inject._
import services.SimbaAlias._
import services.SimbaHTMLHelper.addressToString
@Singleton
class CustomerModel @Inject()(dao: Dao) {


  // TODO: make those methods return IO[_], empowering women
  def getAllCustomerTableRows: Seq[Customer] = {
    dao.getAllCustomers.unsafeRunSync()
  }

  def getAllCustomerTableRowsLike(search: String): Seq[Customer] = {
    dao.getAllCustomerTableRowsLike(search).unsafeRunSync
  }

  def insert(c: CustomerInput): (Boolean, ID) = {
    val id = dao.insertCustomer(c).unsafeRunSync()
    (true, id)
  }

  def findByID(id: ID): CustomerInput = {
    (for {
      c <- dao.getCustomerById(id)
      addresses <- dao.getAllCustomersAddresses(c.id.head).map(_.map(addressToString))
    } yield CustomerInput(
      c.id, c.firstName, c.lastName,
      c.phone, c.phoneNote,
      c.phone2, c.phoneNote2,
      c.instagram, c.preferences, c.notes,
      addresses.toList)).unsafeRunSync()
  }

  def editCustomer(id: ID, c: CustomerInput): Boolean = {
    dao.editCustomer(id, c).unsafeRunSync()
    true
  }

  def getDataForJsonToDisplayInOrderByID(id: ID): IO[CustomerAddressesForJson] = {
    for {
      customer <- dao.getCustomerById(id)
      addresses <- dao.getAllCustomersAddresses(id)
    } yield CustomerAddressesForJson(customer, addresses)
  }

  def getDataForJsonToDisplayInOrder(search: String): IO[Seq[CustomerAddressesForJson]] = {
    IO(getAllCustomerTableRowsLike(search).map { c =>
      CustomerAddressesForJson(c, dao.getAllCustomersAddresses(c.id.head).unsafeRunSync())
    })
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

case class CustomerAddressesForJson(customer: Customer, addresses: Seq[Address])
