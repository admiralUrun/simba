package models

import cats.effect.IO
import doobie._
import doobie.implicits._
import javax.inject._
import cats.implicits._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json._
import services.SimbaHTMLHelper.stringToAddress
import services.SimbaHTMLHelper.addressToString

@Singleton
class CustomerModel @Inject()(dS: DoobieStore) {
  type  JSONWrites[T] = OWrites[T]
  protected val xa: DataSourceTransactor[IO] = dS.getXa()

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

  def insert(c: CustomerForEditAndCreate): Boolean = {
    val customerWithID = (for {
      _ <-
        sql"""insert into customers
           (first_name, last_name,
           phone, phone_note, phone2, phone2_note,
           city, address, flat, entrance, floor,
           instagram, preferences, notes)
           values (${c.firstName}, ${c.lastName},
                ${c.phone}, ${c.phoneNote}, ${c.phone2}, ${c.phoneNote2},
                ${c.instagram},
                ${c.preferences}, ${c.notes})""".update.run
      id <- sql"select lastval()".query[Int].unique
      c <- sql"select * from customers where id = $id".query[Customer].unique
    } yield c)
      .transact(xa)
      .unsafeRunSync()
    insertAddresses(decodeAddressString(c.addresses), customerWithID.id.head)
  }

  def findByID(id: Int): CustomerForEditAndCreate = {
    val c = sql"select * from customers where id = $id"
      .query[Customer]
      .option
      .transact(xa)
      .unsafeRunSync().head

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
      .unsafeRunSync() == 1 && editAddresses(decodeAddressString(c.addresses), c.addressesToDelete.getOrElse(List()), c.id.head)
  }

  def insertAddresses(list: List[Address], customerId: Int): Boolean = {
    insertAddressesReturnConfectionIOListOfInt(list, customerId).transact(xa).unsafeRunSync().forall(_ == 1)
  }

  private def insertAddressesReturnConfectionIOListOfInt(list: List[Address], customerId: Int): ConnectionIO[List[Int]] = {
    list.traverse { a =>
      sql"""insert into addresses (customer_id, city, residential_complex, address, entrance, floor, flat, note_for_courier)
            value (customer_id = $customerId, city = ${a.city}, residential_complex = ${a.residentialComplex}, address = ${a.address}, entrance = ${a.entrance}, floor = ${a.floor}, flat = ${a.flat}, note_for_courier = ${a.notesForCourier})"""
        .update
        .run
    }
  }

  def editAddresses(list: List[Address], listToDelete: List[Int], customerId: Int): Boolean = {
    def insertAddressesReturnConfectionIO(list: List[Address], customerId: Int): ConnectionIO[Int] = {
      insertAddressesReturnConfectionIOListOfInt(list, customerId).map(_.sum)
    }

    def deleteAddresses(list: List[Int]): ConnectionIO[Int] = {
      list.traverse { id =>
        sql"""delete addresses where id = $id""".update.run
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
                  note_for_courier = ${a.notesForCourier} where id = ${a.id}, customer_id = $customerId""".update.run
      }.map(_.sum)
    }

    val customerAddressesInDB = sql""""select * from addresses where customer_id = $customerId"""".query[Address].to[List].transact(xa).unsafeRunSync()

    if (customerAddressesInDB.isEmpty) insertAddresses(list, customerId)
    else {
      (deleteAddresses(listToDelete) *>
        insertAddressesReturnConfectionIO(list.filter(p = a => a.id == null && a.customerId == null), customerId) *>
        editAddresses(list.filter(a => a.id != null && a.customerId != null), customerId)
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

  def getDataForJsonToDisplayInOrder(search: String): JsValue = {
    import play.api.libs.functional.syntax._
    implicit val customerReads: Writes[Customer] = (
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

    implicit val readerAddress: Writes[Address] = (
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

    implicit val readerCustomerAddressesToJson: Writes[CustomerAddressesToJson] = (
      (JsPath \ "customer").write[Customer] and
        (JsPath \ "addresses").write[Seq[Address]]
    )(unlift(CustomerAddressesToJson.unapply))
    Json.toJson(getAllCustomerTableRowsWhere(search).map(c => CustomerAddressesToJson(c, getAllCustomersAddresses(c.id.head))))
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