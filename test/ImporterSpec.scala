import org.specs2.mutable.Specification
import models.Customer
import services.importer.Importer

class ImporterSpec extends Specification {
  val importer = new Importer
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
    val customersTableProperties = Map (
      "firstName" -> "first_name",
      "lastName" ->  "last_name",
      "phone" -> "phone",
      "phoneNote" -> "phone_note",
      "phone2" -> "phone2",
      "phoneNote2" -> "phone2_note",
      "city" -> "city",
      "address" -> "address",
      "flat" -> "flat",
      "entrance" -> "entrance",
      "floor" -> "floor",
      "instagram" -> "instagram",
      "preferences" -> "preferences",
      "notes" ->  "notes"
    )
    importer.generateInsertingProperties(List(), customersTableProperties) must_=== "()"
    importer.generateInsertingProperties(List("firstName", "lastName", "phone"), customersTableProperties) must_=== "(first_name, last_name, phone)"
    importer.generateInsertingProperties(List("firstName", "lastName", "phone", "phoneNote"), customersTableProperties) must_=== "(first_name, last_name, phone, phone_note)"
    importer.generateInsertingProperties(List("phoneNote", "phone", "lastName", "firstName"), customersTableProperties) must_=== "(phone_note, phone, last_name, first_name)"
  }

}
