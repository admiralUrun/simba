package models

import cats.effect.IO
import doobie._
import doobie.implicits._
import javax.inject._
import cats.implicits._

@Singleton
class CustomerModel @Inject()(dS: DoobieStore) {
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

  def insert(c: CustomerForEditAndCreate): Boolean = { // TODO could be pretty idiotic
    val customerWithID = (for {
      _ <- sql"""insert into customers
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
    insertAddresses(decodeAddressesString(c.addresses), customerWithID.id.head)
  }

  def findByID(id: Int): CustomerForEditAndCreate = {
    val c = sql"select * from customers where id = $id"
      .query[Customer]
      .option
      .transact(xa)
      .unsafeRunSync().head

    val a = sql"select (id, city, address, entrance, floor, flat, note_for_courier) from customers_addresses join addresses where customer_id = ${id}"
      .query[Address]
      .to[List]
      .transact(xa)
      .unsafeRunSync

    CustomerForEditAndCreate(
      c.id, c.firstName, c.lastName,
      c.phone, c.phoneNote,
      c.phone2, c.phoneNote2,
      c.instagram, c.preferences, c.notes,
      encodeAddressesToString(a))
  }

  def editCustomer(id: Int, c: CustomerForEditAndCreate): Boolean = { // TODO
    sql"""update customers set first_name = ${c.firstName}, last_name = ${c.lastName},
         phone = ${c.phone}, phone_note = ${c.phoneNote},
         phone2 = ${c.phone2}, phone2_note = ${c.phoneNote2},
         instagram = ${c.instagram},
         preferences = ${c.preferences}, notes = ${c.notes}
         where id = $id"""
      .update
      .run
      .transact(xa)
      .unsafeRunSync() == 1 && insertAddresses(decodeAddressesString(c.addresses), c.id.head)
  }

  def insertAddresses(list: List[Address], customerId: Int): Boolean = {
    list.traverse { a =>
      sql"""insert into addresses (customer_id, city, residential_complex, address, entrance, floor, flat, note_for_courier)
            value (customer_id = $customerId, city = ${a.city}, residential_complex = ${a.residentialComplex}, address = ${a.address}, entrance = ${a.entrance}, floor = ${a.floor}, flat = ${a.flat}, note_for_courier = ${a.notesForCourier})"""
        .update
        .run
    }.transact(xa).unsafeRunSync().forall(_ == 1)
  }

  def decodeAddressesString(a: String): List[Address] = {
    def stringToAddress(s: String): Address = {
      val array = s.split(",")

      def searchInStringFor(search: String): Option[String] = {
        val o = array.find(_.contains(search))
        if (o.isDefined) Option(o.head.replace(search, ""))
        else null
      }

      Address(null, null, array(0).replace("(city)", ""), searchInStringFor("residentialComplex"), array(1).replace("(address)", ""),
        searchInStringFor("entrance"),
        searchInStringFor("floor"),
        searchInStringFor("flat"),
        searchInStringFor("notes"))
    }

    a.split(",").map(stringToAddress).toList
  }

  def encodeAddressesToString(l: List[Address]): String = {
    l.map { a =>
      "(" + List("city", "residentialComplex", "address", "entrance", "floor", "flat", "notes").zip(a.productIterator.toList).filter(_._2 == null)
        .map { t =>
          t._2.toString + t._1
        }.mkString(",") + ")" // (Київ(city),  Волинська10(address) ...)
    }.mkString(",")
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
                                    addresses: String)

case class Address(id: Option[Int], customerID: Option[Int],
                   city: String,
                   residentialComplex: Option[String],
                   address: String,
                   entrance: Option[String],
                   floor: Option[String],
                   flat: Option[String],
                   notesForCourier: Option[String])