package models
import doobie._
import doobie.implicits._
import javax.inject._

@Singleton
class CustomerModel @Inject()(dM: DoobieModel) {
  protected val xa = dM.getXa()
  def getAllTableRows: Seq[Customer] = {
    sql"select * from customers"
      .query[Customer]
      .to[List]
      .transact(xa)
      .unsafeRunSync
  }

  def insert(c: Customer): Unit = {
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
      .unsafeRunAsyncAndForget()
  }

  def findByID(id: String): Customer = { // TODO: Ask senior how to make it better
    sql"select * from customers where id = ${id}"
      .query[Customer]
      .to[List]
      .transact(xa)
      .unsafeRunSync().head
  }
  def editCustomer(c: Customer): Unit = { // TODO fix it
    sql"""update customers set first_name = ${c.firstName}, last_name = ${c.lastName},
         phone = ${c.phone}, phone_note = ${c.phoneNote},
         phone2 = ${c.phone2}, phone2_note = ${c.phoneNote2},
         city = ${c.city}, address = ${c.address}, flat = ${c.flat}, entrance = ${c.entrance}, floor = ${c.floor},
         instagram = ${c.instagram},
         preferences = ${c.preferences}, notes = ${c.notes}
         where id = ${c.id}"""
      .update
      .run
      .transact(xa)
      .unsafeRunAsyncAndForget()
  }
}
case class Customer(id: Option[String],
                    firstName:String, lastName:Option[String],
                    phone:String, phoneNote:Option[String],
                    phone2:Option[String], phoneNote2:Option[String],
                    city:String, address:String, flat: Option[String], entrance:Option[String], floor:Option[String],
                    instagram:Option[String],
                    preferences:Option[String], notes:Option[String])