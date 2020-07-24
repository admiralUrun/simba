package models

import cats.effect.IO
import doobie._
import doobie.implicits._
import javax.inject._
import cats.implicits._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.SimbaHTMLHelper.stringToAddress
import services.SimbaHTMLHelper.addressToString

@Singleton
class CustomerModel @Inject()(dS: DoobieStore) {
  type  JSONWrites[T] = OWrites[T]
  protected val xa: DataSourceTransactor[IO] = dS.getXa()
 private implicit val customerWriter: Writes[Customer] = (
    ( JsPath \ "id").writeNullable[Int] and
      ( JsPath \ "firstName").write[String] and
      ( JsPath \ "lastName").writeNullable[String] and
      ( JsPath \ "phone").write[String] and
      ( JsPath \ "phoneNote").writeNullable[String] and
      ( JsPath \ "phone2").writeNullable[String] and
      ( JsPath \ "phoneNote2").writeNullable[String] and
      ( JsPath \ "instagram").writeNullable[String] and
      ( JsPath \ "preferences").writeNullable[String] and
      ( JsPath \ "notes").writeNullable[String]
    )(unlift(Customer.unapply))

  private implicit val addressWriter: Writes[Address] = (
    ( JsPath \ "id").writeNullable[Int] and
      ( JsPath \ "customerId").writeNullable[Int] and
      ( JsPath \ "city").write[String] and
      ( JsPath \ "residentialComplex").writeNullable[String] and
      ( JsPath \ "address").write[String] and
      ( JsPath \ "entrance").writeNullable[String] and
      ( JsPath \ "floor").writeNullable[String] and
      ( JsPath \ "flat").writeNullable[String] and
      ( JsPath \ "notesForCourier").writeNullable[String]
    )(unlift(Address.unapply))

  private implicit val customerAddressesToJsonWriter: Writes[CustomerAddressesToJson] = (
    (JsPath \ "customer").write[Customer] and
      (JsPath \ "addresses").write[Seq[Address]]
    )(unlift(CustomerAddressesToJson.unapply))

  def getAllCustomerTableRows: Seq[Customer] = {
    sql"select * from customers"
      .query[Customer]
      .to[List]
      .transact(xa)
      .unsafeRunSync
  }

  def getAllCustomerTableRowsWhere(search: String): Seq[Customer] = {
    sql"""select * from customers where
          first_name like $search or
           last_name like $search or
            phone like $search or
             phone2 like $search or
              instagram like $search"""
      .query[Customer]
      .to[List]
      .transact(xa)
      .unsafeRunSync
  }

  def insert(c: CustomerForEditAndCreate): (Boolean, Int) = {
    val customerID = (for {
      _ <- sql"""insert into customers
            (first_name, last_name,
            phone, phone_note, phone2, phone2_note,
           instagram, preferences, notes)
           values (${c.firstName}, ${c.lastName},
                ${c.phone}, ${c.phoneNote}, ${c.phone2}, ${c.phoneNote2},
                ${c.instagram},
                ${c.preferences}, ${c.notes})""".update.run
      id <- sql"select LAST_INSERT_ID()".query[Int].unique
    } yield id)
      .transact(xa)
      .unsafeRunSync()
    (insertAddresses(decodeAddressString(c.addresses), customerID), customerID)
  }

  def insertAddresses(list: List[Address], customerId: Int): Boolean = {
    val connectionIO = insertAddressesReturnConnectionIOListOfInt(list, customerId)
    connectionIO
        .transact(xa)
        .unsafeRunSync().forall(_ == 1)
  }

  def findByID(id: Int): CustomerForEditAndCreate = {
    val c = getCustomerById(id)

    CustomerForEditAndCreate(
      c.id, c.firstName, c.lastName,
      c.phone, c.phoneNote,
      c.phone2, c.phoneNote2,
      c.instagram, c.preferences, c.notes,
      encodeAddressesToString(getAllCustomersAddresses(c.id.head)), Option(null))
  }

  def editCustomer(id: Int, c: CustomerForEditAndCreate): Boolean = {
    sql"""update customers set first_name = ${c.firstName}, last_name = ${c.lastName},
         phone = ${c.phone}, phone_note = ${c.phoneNote},
         phone2 = ${c.phone2}, phone2_note = ${c.phoneNote2},
         instagram = ${c.instagram},
         preferences = ${c.preferences}, notes = ${c.notes}
         where id = $id"""
      .update
      .run
      .transact(xa)
      .unsafeRunSync() == 1 &&
      editAddresses(decodeAddressString(c.addresses), c.addressesToDelete.getOrElse(List()), id)
  }

  def editAddresses(list: List[Address], listToDelete: List[Int], customerId: Int): Boolean = {
    def insertAddressesReturnConfectionIO(list: List[Address], customerId: Int): ConnectionIO[Int] = {
      insertAddressesReturnConnectionIOListOfInt(list, customerId).map(_.sum)
    }

    def deleteAddresses(list: List[Int]): ConnectionIO[Int] = {
      list.traverse { id =>
        sql"""delete from addresses where id = $id""".update.run
      }.map(_.sum)
    }

    def editAddresses(list: List[Address], customerId: Int): ConnectionIO[Int] = {
      list.traverse { a =>
        sql"""update addresses set city = ${a.city},
              residential_complex = ${a.residentialComplex},
               address = ${a.address},
                entrance = ${a.entrance},
                 floor = ${a.floor},
                  flat = ${a.flat},
                  note_for_courier = ${a.notesForCourier} where id = ${a.id} and customer_id = $customerId""".update.run
      }.map(_.sum)
    }

    val customerAddressesInDB = sql"""select * from addresses where customer_id = $customerId""".query[Address].to[List].transact(xa).unsafeRunSync()

    if (customerAddressesInDB.isEmpty) insertAddresses(list, customerId)
    else {
      (deleteAddresses(listToDelete) *>
        insertAddressesReturnConfectionIO(list.filter(p = a => a.id.isEmpty && a.customerId.isEmpty), customerId)  *>
         editAddresses(list.filter(a => a.id.isDefined && a.customerId.isDefined), customerId)
        ).transact(xa)
        .unsafeRunSync() >= list.length
    }
  }

  def getAllCustomersAddresses(customerId: Int): List[Address] = {
    sql"select * from addresses where customer_id = $customerId"
      .query[Address]
      .to[List]
      .transact(xa)
      .unsafeRunSync
  }

  def getDataForJsonToDisplayInOrderByID(id: Int): JsValue = {
    val c = getCustomerById(id)
    Json.toJson(CustomerAddressesToJson(c, getAllCustomersAddresses(id)))
  }

  def getDataForJsonToDisplayInOrder(search: String): JsValue = Json.toJson(getAllCustomerTableRowsWhere(search).map{ c =>
    CustomerAddressesToJson(c, getAllCustomersAddresses(c.id.head))
  })

  private def insertAddressesReturnConnectionIOListOfInt(list: List[Address], customerId: Int): ConnectionIO[List[Int]] = {
    list.traverse { a =>
      sql"""insert into addresses (customer_id, city, residential_complex, address, entrance, floor, flat, note_for_courier)
            value (${customerId}, ${a.city}, ${a.residentialComplex}, ${a.address}, ${a.entrance}, ${a.floor}, ${a.flat}, ${a.notesForCourier})"""
        .update
        .run
    }
  }

  private def getCustomerById(id: Int): Customer = {
    sql"select * from customers where id = $id"
      .query[Customer]
      .unique
      .transact(xa)
      .unsafeRunSync()
  }

  private def decodeAddressString(a: List[String]): List[Address] = {
    a.map(stringToAddress)
  }

  private def encodeAddressesToString(l: List[Address]): List[String] = {
    l.map(addressToString)
  }

}

case class Customer(id: Option[Int],
                    firstName: String, lastName: Option[String],
                    phone: String, phoneNote: Option[String],
                    phone2: Option[String], phoneNote2: Option[String],
                    instagram: Option[String],
                    preferences: Option[String], notes: Option[String])

case class CustomerForEditAndCreate(id: Option[Int],
                                    firstName: String, lastName: Option[String],
                                    phone: String, phoneNote: Option[String],
                                    phone2: Option[String], phoneNote2: Option[String],
                                    instagram: Option[String],
                                    preferences: Option[String], notes: Option[String],
                                    addresses: List[String],
                                    addressesToDelete: Option[List[Int]])

case class Address(id: Option[Int], customerId: Option[Int],
                   city: String,
                   residentialComplex: Option[String],
                   address: String,
                   entrance: Option[String],
                   floor: Option[String],
                   flat: Option[String],
                   notesForCourier: Option[String])

case class CustomerAddressesToJson(customer: Customer, addresses: Seq[Address])