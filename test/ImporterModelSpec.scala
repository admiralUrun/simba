import org.specs2.mutable.Specification
import models.{Customer, ImporterModel}

class ImporterModelSpec extends Specification {
  val importer = new ImporterModel
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

  "getListInBraces return right string" >> {
    importer.getListInBraces(List()) must_=== "()"
    importer.getListInBraces(List(), withSQLBraces = true) must_=== "()"
    importer.getListInBraces(List("doobie.implicits", "Messages", "unstoppable", "t2"), withSQLBraces = true) must_=== "('doobie.implicits', 'Messages', 'unstoppable', 't2')"
    importer.getListInBraces(List("must_", "tonight", "Someone", "crying", "long", "Hey")) must_=== "(must_, tonight, Someone, crying, long, Hey)"
    importer.getListInBraces(List("way", "way")) must_!= "(way, way,)"
    importer.getListInBraces(List("way", "way")) must_=== "(way, way)"
    importer.getListInBraces(List("07309", "gwgwgwgwgw", "how", "hmmm"), withSQLBraces = true) must_!= "('07309', 'gwgwgwgwgw', 'how', 'hmmm',)"
    importer.getListInBraces(List("07309", "gwgwgwgwgw", "how", "hmmm"), withSQLBraces = true) must_=== "('07309', 'gwgwgwgwgw', 'how', 'hmmm')"
  }

  "generateInsertingVariables generate right fragment" >> {
    importer.generateInsertingVariables(List()) must_=== " values ()"
    importer.generateInsertingVariables(List("07309", "gwgwgwgwgw", "how", "hmmm")) must_=== " values ('07309', 'gwgwgwgwgw', 'how', 'hmmm')"
    importer.generateInsertingVariables(List("412633384", "me", "where is", "fool")) must_=== " values ('412633384', 'me', 'where is', 'fool')"
  }

  "generateInsertingProperties generate right fragment" >> {
    importer.generateInsertingProperties(List()) must_=== "()"
    importer.generateInsertingProperties(List("firstName", "lastName", "phone")) must_=== "(first_name, last_name, phone)"
    importer.generateInsertingProperties(List("firstName", "lastName", "phone", "phoneNote")) must_=== "(first_name, last_name, phone, phone_note)"
    importer.generateInsertingProperties(List("phoneNote", "phone", "lastName", "firstName")) must_=== "(phone_note, phone, last_name, first_name)"
  }

  "takePropertiesFormCustomer return right List" >> {
    importer.takePropertiesFormCustomer(customer1) must_=== List("firstName", "lastName", "phone", "city", "address")
    importer.takePropertiesFormCustomer(customer2) must_=== List("firstName", "lastName", "phone", "phoneNote", "city", "address", "flat", "entrance", "floor", "preferences", "notes")
    importer.takePropertiesFormCustomer(customer3) must_=== List("firstName", "lastName", "phone", "phoneNote", "city", "address", "flat", "entrance", "floor", "preferences")
  }

  "takeVariablesFormCustomer return right List" >> {
    importer.takeVariablesFormCustomer(customer1) must_=== List("Andrii", "Yakovenko", "08415512", "Kiyv", "SomeWhere")
    importer.takeVariablesFormCustomer(customer2) must_=== List("Andrii", "Yakovenko", "08415512", "phone", "Kiyv", "SomeWhere", "11", "88", "526662", "lol", "he is")
    importer.takeVariablesFormCustomer(customer3) must_=== List("Andrii", "Yakovenko", "08415512", "phone", "Kiyv", "SomeWhere", "11", "88", "526662", "lol")
  }

  "takePropertiesFormCustomer & takeVariablesFormCustomer return equal length for one Customer" >> {
    importer.takeVariablesFormCustomer(customer1).length must_=== importer.takePropertiesFormCustomer(customer1).length
    importer.takeVariablesFormCustomer(customer2).length must_=== importer.takePropertiesFormCustomer(customer2).length
    importer.takeVariablesFormCustomer(customer3).length must_=== importer.takePropertiesFormCustomer(customer3).length
  }

  "generateInsertForCustomer generate right command" >> {
    importer.generateInsertForCustomer(customer1) must_=== "insert into customers (first_name, last_name, phone, city, address) values ('Andrii', 'Yakovenko', '08415512', 'Kiyv', 'SomeWhere');"
    importer.generateInsertForCustomer(customer2) must_=== "insert into customers (first_name, last_name, phone, phone_note, city, address, flat, entrance, floor, preferences, notes) values ('Andrii', 'Yakovenko', '08415512', 'phone', 'Kiyv', 'SomeWhere', '11', '88', '526662', 'lol', 'he is');"
    importer.generateInsertForCustomer(customer3) must_=== "insert into customers (first_name, last_name, phone, phone_note, city, address, flat, entrance, floor, preferences) values ('Andrii', 'Yakovenko', '08415512', 'phone', 'Kiyv', 'SomeWhere', '11', '88', '526662', 'lol');"
  }
}
