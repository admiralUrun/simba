package dao

import java.util.Date
import javax.inject.Inject
import cats.effect.IO
import cats.implicits._
import doobie._
import doobie.implicits._
import models._
import services.SimbaAlias.ID
import services.SimbaHTMLHelper.stringToAddress


class Dao @Inject()(dS: DoobieStore) {
  private val xa = dS.getXa

  private val customerSelect = sql"select id, first_name, last_name, phone, phone2_note, phone2, phone2_note, instagram, preferences, notes from customers "
  private val addressSelect = sql"select id, customer_id, city, residential_complex, address, entrance, floor, flat, delivery_notes from addresses "
  private val orderSelect = sql"select id, customer_id, address_id, inviter_id, order_day, delivery_day, deliver_from, deliver_to, out_of_zone_delivery, delivery_on_monday, total, discount,  payment, paid, delivered, note from orders "
  private val offerSelect = sql"select id, name, price, expiration_date, menu_type from offers "
  private val recipesSelect = sql"select id, name, type, edited from recipes "

  def getAllCustomers: IO[Seq[Customer]] = customerSelect
    .query[Customer]
    .to[List]
    .transact(xa)

  def getAllCustomerTableRowsLike(search: String): IO[Seq[Customer]] = (customerSelect ++
    fr"""where first_name like $search or
           last_name like $search or
            phone like $search or
             phone2 like $search or
              instagram like $search""").query[Customer]
    .to[List]
    .transact(xa)

  def getAllCustomersAddresses(customerId: ID): IO[Seq[Address]] = (addressSelect ++ fr"where customer_id = $customerId")
    .query[Address]
    .to[List]
    .transact(xa)

  def getCustomerBy(id: ID): IO[Customer] = (customerSelect ++ fr"where id = $id")
    .query[Customer]
    .unique
    .transact(xa)

  def getAddressBy(id: ID): IO[Address] = (addressSelect ++ fr"where id = $id")
    .query[Address]
    .unique
    .transact(xa)

  def getAllOrders: IO[Seq[Order]] = orderSelect
    .query[Order]
    .to[List]
    .transact(xa)

  def getOrderBy(id: ID): IO[Order] = (orderSelect ++ fr"where id = $id")
    .query[Order]
    .unique
    .transact(xa)

  def getAllOfferIdByOrder(id: ID): IO[List[Offer]] = {
    val orderOffers = sql"select distinct offer_id from order_recipes where order_id = $id".query[ID].to[List].transact(xa).unsafeRunSync()
    orderOffers.traverse { id =>
      (offerSelect ++ fr"where id = $id").query[Offer].to[List]
    }.transact(xa).map(_.flatten)
  }

  def getOffersByDate(date: String): IO[Seq[Offer]] = {
    (offerSelect ++ fr"where expiration_date = $date")
      .query[Offer]
      .to[List]
      .transact(xa)
  }

  def getOfferByDateAndMenuType(date: Date, menuType: Int): IO[Seq[Offer]] = (offerSelect ++ fr"where expiration_date = $date" ++ fr" and menu_type = $menuType")
    .query[Offer]
    .to[List]
    .transact(xa)

  def getRecipesLike(name: String): IO[Seq[Recipe]] = (recipesSelect ++ fr"where name like $name")
    .query[Recipe]
    .to[Seq]
    .transact(xa)

  def getRecipesBy(ids: List[ID]): IO[Seq[Recipe]] = ids.traverse { id =>
    (recipesSelect ++ fr"where id = $id")
      .query[Recipe]
      .unique
  }.transact(xa)

  def getDiscountFormCustomerBy(id: ID, sunAndMonLasWeek:(Date, Date)): IO[Int] = {
    for {
      ordersLastSunday  <- (orderSelect ++ fr"where inviter_id = $id and delivery_day = ${sunAndMonLasWeek._1}").query[Order].to[List].transact(xa)
      ordersLastMonday  <- (orderSelect ++ fr"where inviter_id = $id and delivery_day = ${sunAndMonLasWeek._2}").query[Order].to[List].transact(xa)
    } yield ordersLastSunday.length + ordersLastMonday.length
  }

  def getCalculationsOnThisWeek(dates: (Date, Date)): Seq[Calculation] = {
    sql"""select i.description, i.unit, i.art_by,
           sum(if(i.unit ='шт', r_i.netto, o_r.quantity * r_i.netto)) as count®
         from orders
             join order_recipes o_r on orders.id = o_r.order_id
             join recipes r on o_r.recipe_id = r.id
             join recipe_ingredients r_i on r.id = r_i.recipe_id
             join ingredients i on r_i.ingredient_id = i.id
             where orders.delivery_day = ${dates._1} or orders.delivery_day = ${dates._2}
             group by i.id""".query[Calculation].to[List].transact(xa).unsafeRunSync()
  }

  // --- Change methods ---

  def insertCustomer(c: CustomerInput): IO[ID] = (for {
    _ <-
      sql"""insert into customers
            (first_name, last_name,
            phone, phone_note, phone2, phone2_note,
           instagram, preferences, notes)
           values (${c.firstName}, ${c.lastName},
                ${c.phone}, ${c.phoneNote}, ${c.phone2}, ${c.phoneNote2},
                ${c.instagram},
                ${c.preferences}, ${c.notes})""".update.run
    id <- sql"select last_insert_id()".query[ID].unique
    _ <- insertAddresses(c.addresses.map(stringToAddress), id)
  } yield id)
    .transact(xa)

  def insertOrder(o: OrderInput, convertStringToMinutes: String => Int, convertPayment: String => Int): IO[Unit] = {
    val recipesIdsAndQuantity = getAllOffersRecipes(o.inOrder).unsafeRunSync()
    (for {
      _ <-
        sql"""insert into orders (customer_id, address_id, inviter_id,
                                    order_day, delivery_day,
                                    deliver_from, deliver_to,
                                    total, discount,
                                    payment,
                                    out_of_zone_delivery, delivery_on_monday,
                                    paid, delivered, note)
                                     values (${o.customerId}, ${o.addressId}, ${o.inviterId},
                                              ${o.orderDay}, ${o.deliveryDay},
                                              ${convertStringToMinutes(o.deliverFrom)}, ${convertStringToMinutes(o.deliverTo)},
                                              ${o.total}, ${o.discount},
                                              ${convertPayment(o.payment)},
                                              ${o.offlineDelivery}, ${o.offlineDelivery},
                                              ${o.paid}, ${o.delivered},
                                              ${o.note}) """.update.run
      id <- sql"select last_insert_id()".query[ID].unique
      _ <- insertOrderRecipes(id, recipesIdsAndQuantity)
    } yield ()).transact(xa)
  }

  def insertOrUpdateOffers(date: Date, menuType: Int, list: List[InsertOffer]): IO[Unit] = {
    def deleteBeforeUpdate(date: Date, menuType: Int): ConnectionIO[Int] = { // TODO: select and delete if needed to fix join table bugs
      sql"delete from offers where expiration_date = $date and menu_type = $menuType".update.run
    }

    def insertOrUpdateOffer(name: String, date: Date, menuType: Int, price: Int, recipes: List[Recipe], quantityOfRecipes: Int): ConnectionIO[Unit] = {
      for {
        _ <- sql"insert into offers (name, price, expiration_date, menu_type) values ($name, $price, $date, $menuType)".update.run
        id <- sql"select last_insert_id()".query[Int].unique
        _ <- recipes.traverse { r =>
          sql"insert into offer_recipes (offer_id, recipe_id, quantity) values ($id, ${r.id}, $quantityOfRecipes)".update.run
        }
      } yield ()
    }

    (for {
      _ <- deleteBeforeUpdate(date, menuType)
      _ <- list.traverse { o =>
        insertOrUpdateOffer(o.name, date, menuType, o.price, o.recipes, o.multipliepQuantity)
      }
    } yield ()).transact(xa)
  }

  def editCustomer(id: ID, c: CustomerInput): IO[Unit] = {
    def editAddresses(list: List[Address], customerId: ID): ConnectionIO[Unit] = {
      sql"delete from addresses where customer_id = $customerId".update.run *>
        insertAddresses(list, customerId)
    }

    (sql"""update customers set
        first_name = ${c.firstName}, last_name = ${c.lastName},
         phone = ${c.phone}, phone_note = ${c.phoneNote},
         phone2 = ${c.phone2}, phone2_note = ${c.phoneNote2},
         instagram = ${c.instagram},
         preferences = ${c.preferences}, notes = ${c.notes}
         where id = $id""".update.run *>
      editAddresses(c.addresses.map(stringToAddress), id)).transact(xa)
  }

  def editOrder(id: ID, o: OrderInput, convertStringToMinutes: String => Int, convertPayment: String => Int): IO[Unit] = {
    val recipesIdsAndQuantity = getAllOffersRecipes(o.inOrder).unsafeRunSync()
    (for {
      _ <-
        sql"""update orders set
           address_id = ${o.addressId},
           inviter_id = ${o.inviterId},
            delivery_day = ${o.deliveryDay},
             deliver_from = ${convertStringToMinutes(o.deliverFrom)}, deliver_to = ${convertStringToMinutes(o.deliverTo)},
              total = ${o.total}, discount = ${o.discount},
               payment = ${convertPayment(o.payment)},
              out_of_zone_delivery = ${o.offlineDelivery}, delivery_on_monday = ${o.deliveryOnMonday},
              paid = ${o.paid}, delivered = ${o.delivered},
              note = ${o.note} where id = $id""".update.run
      _ <- sql"delete from order_recipes where order_id = $id".update.run
      _ <- insertOrderRecipes(id, recipesIdsAndQuantity)
    } yield ()).transact(xa)
  }

  def updateOffersNameAndPrice(list: List[(Int, (String, Int))]): IO[Unit] = list.traverse { case (id, (name, price)) =>
    sql"update offers set name = $name, price= $price where id= $id".update.run
  }.transact(xa).void

  // --- Private methods ---

  private def getAllOffersRecipes(ids: List[Int]): IO[List[(ID, Int, Int)]] = ids.traverse { offerId =>
    sql"select * from offer_recipes where offer_id = $offerId".query[OfferRecipes].to[List]
  }.map(_.flatten).transact(xa).map(_.map(o => (o.offerId, o.recipesId, o.quantity)))

  private def insertAddresses(list: List[Address], customerId: ID): ConnectionIO[Unit] = list.traverse { a =>
    sql"""insert into addresses (customer_id, city, residential_complex, address, entrance, floor, flat, delivery_notes)
            value ($customerId, ${a.city}, ${a.residentialComplex}, ${a.address}, ${a.entrance}, ${a.floor}, ${a.flat}, ${a.notesForCourier})"""
      .update
      .run
  }.void

  private def insertOrderRecipes(orderId: Int, recipesIdsQuantity: List[(Int, Int, Int)]): ConnectionIO[Unit] = recipesIdsQuantity.traverse { rIdQ =>
    sql"insert into order_recipes (order_id, offer_id, recipe_id, quantity) value ($orderId, ${rIdQ._1}, ${rIdQ._2}, ${rIdQ._3})".update.run
  }.void

}

