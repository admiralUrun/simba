package Dao

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
  private val orderSelect = sql"select id, customer_id, address_id, order_day, delivery_day, deliver_from, deliver_to, out_of_zone_delivery, delivery_on_monday, total, payment, paid, delivered, note from orders "
  private val offerSelect = sql"select id, name, price, execution_date, menu_type from offers "
  private val recipesSelect = sql"select id, name, type, edited from recipes "

  def getAllCustomers: IO[Seq[Customer]] = {
    customerQuery(customerSelect)
      .to[List]
      .transact(xa)
  }
   def getAllCustomerTableRowsLike(search: String): IO[Seq[Customer]] = {
    customerQuery(customerSelect ++
      sql"""where first_name like $search or
           last_name like $search or
            phone like $search or
             phone2 like $search or
              instagram like $search""")
      .to[List]
      .transact(xa)
  }

  def getAllCustomersAddresses(customerId: ID): IO[Seq[Address]] = {
    addressQuery(addressSelect ++ fr"where customer_id = $customerId")
      .to[List]
      .transact(xa)
  }

  def getCustomerById(id: ID): IO[Customer] = {
    customerQuery(customerSelect ++ fr"where id = $id")
      .unique
      .transact(xa)
  }

  def getAddressById(id: ID): IO[Address] = {
    addressQuery(addressSelect ++ fr"where id = $id")
      .unique
      .transact(xa)
  }

  def getAllOrders: IO[Seq[Order]] = {
    orderQuery(orderSelect)
      .to[List]
      .transact(xa)
  }

  def getOrderByID(id: ID): IO[Order] = {
    orderQuery(orderSelect ++ fr"where id = $id")
      .unique
      .transact(xa)
  }

  def getAllOfferIdByOrderId(id: ID): IO[List[Offer]] = {
    sql"select * from order_offers where order_id = $id".query[OrderOffer].to[List].transact(xa).unsafeRunSync().traverse { orderOffer =>
      offerQuery( offerSelect ++ fr"where id = ${orderOffer.offerId}").to[List]
    }.transact(xa).map(_.flatten)
  }

  def getOffersByDate(date : Date): IO[Seq[Offer]] = {
    offerQuery(offerSelect ++ fr"where expiration_date $date")
      .to[List]
      .transact(xa)
  }

  def getOfferByDateAndMenuType(date: Date, menuType: String): IO[Seq[Offer]] = {
    offerQuery(offerSelect ++ fr"where expiration_date $date" ++ fr"and menu_type = $menuType").to[List].transact(xa)
  }

  def getRecipesLike(name: String): IO[Seq[Recipe]] = {
    recipesQuery(recipesSelect ++ fr"where name like $name").to[Seq].transact(xa)
  }

  def getRecipesBy(ids: List[ID]): IO[Seq[Recipe]] = {
    ids.traverse{ id =>
      recipesQuery(recipesSelect ++ fr"where id = $id").unique
    }.transact(xa)
  }

  // --- Change methods ---


  def insertCustomer(c: CustomerInput): IO[ID] = {
    (for {
      _ <- update(sql"""insert into customers
            (first_name, last_name,
            phone, phone_note, phone2, phone2_note,
           instagram, preferences, notes)
           values (${c.firstName}, ${c.lastName},
                ${c.phone}, ${c.phoneNote}, ${c.phone2}, ${c.phoneNote2},
                ${c.instagram},
                ${c.preferences}, ${c.notes})""").run
      id <- sql"select LAST_INSERT_ID()".query[ID].unique
      _ <- insertAddressesReturnConnectionIOListOfInt(c.addresses.map(stringToAddress), id)
    } yield id)
      .transact(xa)
  }

  def insertOrder(o: OrderForEditAndCreate, convertStringToMinutes: String => Int): IO[Unit] = {
    (for {
      _ <- update(sql"""insert into orders (customer_id, address_id,
                                    order_day, delivery_day,
                                    deliver_from, deliver_to,
                                    total,
                                    payment,
                                    out_of_zone_delivery, delivery_on_monday,
                                    paid, delivered, note)
                                     values (${o.customerId}, ${o.addressId},
                                              ${o.orderDay}, ${o.deliveryDay},
                                              ${convertStringToMinutes(o.deliverFrom)}, ${convertStringToMinutes(o.deliverTo)},
                                              ${o.total},
                                              ${o.payment},
                                              ${o.offlineDelivery}, ${o.offlineDelivery},
                                              ${o.paid}, ${o.delivered},
                                              ${o.note}) """).run
      id <- sql"select LAST_INSERT_ID()".query[ID].unique
      recipesIdsAndQuantity <- getAllOffersRecipes(o.inOrder).map(_.map(o => (o.recipesId, o.quantity)))
      _ <- insertOrderRecipes(id, recipesIdsAndQuantity)
      _ <- insertOrderOffers(id, o.inOrder)
    } yield ()).transact(xa)
  }

  def insertOffers(date: Date, menuType: String, list: List[InsertOffer]): IO[Unit] = {
    def deleteBeforeInsets(date: Date, menuType: String): ConnectionIO[Int] = {
      sql"delete from offers where execution_date = $date and menu_type = $menuType".update.run
    }

    def insertOffer(name: String, date: Date, menuType: String, price: Int, recipes: List[Recipe], quantityOfRecipes: Int): ConnectionIO[Unit] = {
      for {
        _ <- sql"insert into offers (name, price, execution_date, menu_type) values ($name, $price, $date, $menuType)".update.run
        id <- sql"select LAST_INSERT_ID()".query[Int].unique
        _ <- recipes.traverse { r =>
          sql"insert into offer_recipes (offer_id, recipe_id, quantity) values ($id, ${r.id}, $quantityOfRecipes)".update.run
        }
      } yield ()
    }

    (for {
      _ <- deleteBeforeInsets(date, menuType)
      _ <- list.traverse{ o =>
        insertOffer(o.name, date, menuType, o.price, o.recipes, o.quantityOfRecipes)
      }
    } yield ()).transact(xa)
  }

  def editCustomer(id: ID, c: CustomerInput): IO[Unit] = {
    def editAddresses(list: List[Address], customerId: ID): ConnectionIO[Unit] = {
      update(sql"delete from addresses where customer_id = $customerId").run *>
        insertAddressesReturnConnectionIOListOfInt(list, customerId)
    }

    (update(sql"""update customers set
        first_name = ${c.firstName}, last_name = ${c.lastName},
         phone = ${c.phone}, phone_note = ${c.phoneNote},
         phone2 = ${c.phone2}, phone2_note = ${c.phoneNote2},
         instagram = ${c.instagram},
         preferences = ${c.preferences}, notes = ${c.notes}
         where id = $id""").run *>
      editAddresses(c.addresses.map(stringToAddress), id)).transact(xa)
  }

  def editOrder(id: ID, o: OrderForEditAndCreate, convertStringToMinutes: String => Int): IO[Unit] = {
    (for {
     _ <- update(sql"""update orders set
           address_id = ${o.addressId},
            delivery_day = ${o.deliveryDay},
             deliver_from = ${convertStringToMinutes(o.deliverFrom)},
              deliver_to = ${convertStringToMinutes(o.deliverTo)},
              total = ${o.total},
              payment = ${o.payment},
              out_of_zone_delivery = ${o.offlineDelivery},
              delivery_on_monday = ${o.deliveryOnMonday},
              paid = ${o.paid},
              delivered = ${o.delivered},
              note = ${o.note} where id = $id
              """).run
      _ <- update(sql"delete from order_offers where order_id = $id").run
      _ <- insertOrderOffers(id, o.inOrder)
      _ <- update(sql"delete from order_recipes where order_id = $id").run
     recipesIdsAndQuantity <- getAllOffersRecipes(o.inOrder).map(_.map(o => (o.recipesId, o.quantity)))
     _ <- insertOrderRecipes(id, recipesIdsAndQuantity)
    } yield()).transact(xa)
  }

  def editOffers(list: List[(Int, (String, Int))]): IO[Unit] = {
    list.traverse { case (id, (name, price)) =>
      sql"update offers set name = $name, price= $price where id= $id".update.run
    }.transact(xa).void
  }


  private def getAllOffersRecipes(ids: List[Int]): ConnectionIO[List[OfferRecipes]] = {
    ids.traverse { offerId =>
      sql"select * from offer_recipes where offer_id = $offerId".query[OfferRecipes].to[List]
    }.map(_.flatten)
  }

  private def insertAddressesReturnConnectionIOListOfInt(list: List[Address], customerId: ID): ConnectionIO[Unit] = {
    list.traverse { a =>
      sql"""insert into addresses (customer_id, city, residential_complex, address, entrance, floor, flat, delivery_notes)
            value ($customerId, ${a.city}, ${a.residentialComplex}, ${a.address}, ${a.entrance}, ${a.floor}, ${a.flat}, ${a.notesForCourier})"""
        .update
        .run
    }.void
  }

  private def insertOrderOffers(orderId: Int, offersInOrder: List[Int]): ConnectionIO[Int] = {
    offersInOrder.traverse { offerId =>
      update(sql"insert into order_offers (order_id, offer_id) values ($orderId, $offerId)").run
    }.map(_.sum)
  }

  private def insertOrderRecipes(orderId: Int, recipesIdsQuantity: List[(Int, Int)]): ConnectionIO[Int] = {
    recipesIdsQuantity.traverse { rIdQ =>
      sql"insert into order_recipes (order_id, recipe_id, quantity) value ($orderId, ${rIdQ._1}, ${rIdQ._2})".update.run
    }.map(_.sum)
  }

  private def customerQuery(cF: Fragment): Query0[Customer] = cF.query[Customer]

  private def addressQuery(aF: Fragment): Query0[Address] = aF.query[Address]

  private def orderQuery(oF: Fragment): Query0[Order] = oF.query[Order]

  private def offerQuery(oF: Fragment): Query0[Offer] = oF.query[Offer]

  private def recipesQuery(rF: Fragment): Query0[Recipe] = rF.query[Recipe]

  private def update[T](fragment: Fragment): Update0 = fragment.update

}

