package models
import cats.effect.IO
import doobie._
import doobie.implicits._
import javax.inject._

@Singleton
class CustomerModel @Inject()(dS: DoobieStore) {
  protected val xa: DataSourceTransactor[IO] = dS.getXa()
  def getAllTableRows: Seq[Customer] = {
    sql"select * from customers"
      .query[Customer]
      .to[List]
      .transact(xa)
      .unsafeRunSync
  }

  def getAllTableRowsWhere(search: String, select: String): Seq[Customer] = {
    def getAllTableRowsWhere(s: String, select: String): Fragment = {
      def getFragmentForPhones(s: String): Fragment = {
        sql"""select * from customers where phone = $s or phone2 = $s
             or phone like $s or phone2 like $s""" // TODO: Ask senior how to do it better
      }

      if (select == "id") sql"select * from customers where id = $s"
      else if (select == "name") sql"select * from customers where fist_name = $s"
      else if (select == "lastName") sql"select * from customers where last_name = $s"
      else if (select == "phones") getFragmentForPhones(s)
      else if (select == "instagram") sql"select * from customers where instagram = $s"
      else sql"select * from customers"
    }

    getAllTableRowsWhere(search, select)
      .query[Customer]
      .to[List]
      .transact(xa)
      .unsafeRunSync
  }

  def insert(c: Customer): Boolean = {
    sql"""insert into customers
           (first_name, last_name,
           phone, phone_note, phone2, phone2_note,
           city, address, flat, entrance, floor,
           instagram, preferences, notes)
           values (${c.firstName}, ${c.lastName},
                ${c.phone}, ${c.phoneNote}, ${c.phone2}, ${c.phoneNote2},
                ${c.city}, ${c.address}, ${c.flat}, ${c.entrance}, ${c.floor},
                ${c.instagram},
                ${c.preferences}, ${c.notes})"""
      .update
      .run
      .transact(xa)
      .unsafeRunSync() == 1
  }

  def findByID(id: Int): Customer = {
    sql"select * from customers where id = $id"
      .query[Customer]
      .option
      .transact(xa)
      .unsafeRunSync().head
  }
  def editCustomer(id: Int, c: Customer): Boolean = {
    sql"""update customers set first_name = ${c.firstName}, last_name = ${c.lastName},
         phone = ${c.phone}, phone_note = ${c.phoneNote},
         phone2 = ${c.phone2}, phone2_note = ${c.phoneNote2},
         city = ${c.city}, address = ${c.address}, flat = ${c.flat}, entrance = ${c.entrance}, floor = ${c.floor},
         instagram = ${c.instagram},
         preferences = ${c.preferences}, notes = ${c.notes}
         where id = $id"""
      .update
      .run
      .transact(xa)
      .unsafeRunSync() == 1
  }
}
case class Customer(id: Option[Int],
                    firstName:String, lastName:Option[String],
                    phone:String, phoneNote:Option[String],
                    phone2:Option[String], phoneNote2:Option[String],
                    city:String, address:String, flat: Option[String], entrance:Option[String], floor:Option[String],
                    instagram:Option[String],
                    preferences:Option[String], notes:Option[String])