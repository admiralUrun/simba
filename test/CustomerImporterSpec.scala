import models.Customer
import org.specs2.mutable.Specification
import models.Customer
import services.importer.CustomerImporter

class CustomerImporterSpec extends Specification {
  val cImporter = new CustomerImporter
  val customer1: Customer = Customer(
    null,
    "Andrii", Option("Yakovenko"),
    "08415512", null,
    null, null,
    "Kiyv", "SomeWhere", null, null, null,
    null, null, null)
  val customer2: Customer = Customer(
    null,
    "Andrii", Option("Yakovenko"),
    "08415512", Option("phone"),
    null, null,
    "Kiyv", "SomeWhere", Option("11"), Option("88"), Option("526662"),
    null, Option("lol"), Option("he is"))
  val customer3: Customer = Customer(
    null,
    "Andrii", Option("Yakovenko"),
    "08415512", Option("phone"),
    null, null,
    "Kiyv", "SomeWhere", Option("11"), Option("88"), Option("526662"),
    null, Option("lol"), null)

  "generateInsertForCustomer generate right command" >> {
    cImporter.generateInsertForCustomer(customer1) must_=== "insert into customers (first_name, last_name, phone, city, address) values ('Andrii', 'Yakovenko', '08415512', 'Kiyv', 'SomeWhere');"
    cImporter.generateInsertForCustomer(customer2) must_=== "insert into customers (first_name, last_name, phone, phone_note, city, address, flat, entrance, floor, preferences, notes) values ('Andrii', 'Yakovenko', '08415512', 'phone', 'Kiyv', 'SomeWhere', '11', '88', '526662', 'lol', 'he is');"
    cImporter.generateInsertForCustomer(customer3) must_=== "insert into customers (first_name, last_name, phone, phone_note, city, address, flat, entrance, floor, preferences) values ('Andrii', 'Yakovenko', '08415512', 'phone', 'Kiyv', 'SomeWhere', '11', '88', '526662', 'lol');"
  }
}
