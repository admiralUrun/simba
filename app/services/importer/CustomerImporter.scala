package services.importer

import java.io.{File, PrintWriter}
import java.util.regex.Pattern

import com.github.tototoshi.csv.CSVReader
import models.{Address, Customer}
import play.api.db.Database
import services.Counter

import scala.annotation.tailrec

class CustomerImporter extends Importer {
  def importCustomersCSV(): Unit = {
    def getAddressWithSeparation(row: Array[String], customerId: Int): Address = {
      val city = if (row(9).isEmpty) "Київ" else row(9)
      val residentialComplex = if (row(10).isEmpty) null else Option(row(10))
      Address(null, Option(customerId),
        city, residentialComplex, address = row(5),
        Option(row(6)), Option(row(7)), Option(row(8)), null)
    }
    def getAddressWithOutSeparation(row: Array[String], customerId: Int): Address = {
      def parseAddress(a: Array[String]): Address = {
        def getCity(string: String): String = if (string.isEmpty || string.contains("ЖК")) "Київ" else string
        def getResidentialComplex(string: String): Option[String] = if (string.contains("ЖК")) Option(string) else null

        if(a.length == 2) Address(null, Option(customerId), a(0), null, a(1), null, null, null, null)
        else if(a.length == 4) Address(null, Option(customerId), "Київ", null, a(0), Option(a(1)), Option(a(2)), Option(a(3)), null)
        else if(a.length == 5) Address(null, Option(customerId), getCity(a(0)), getResidentialComplex(a(0)), a(1), Option(a(3)), Option(a(4)), Option(a(2)), null)
        else if(a.length == 6) Address(null, Option(customerId), getCity(a(0)), getResidentialComplex(a(0)), a(1), Option(a(3)), Option(a(4)), Option(a(2)), Option(a(5)))
        else Address(null, Option(customerId), a(0), null, s"Error with address for $customerId", null, null, null, null)
      }
      parseAddress(row(3).split(','))
    }

    def importRows(lines: List[Array[String]], startWithID: Int, getAddress: (Array[String], Int) => Address): Seq[SQLCommand] = {
      def getCustomer(row: Array[String], id: Int): Customer = {
        def parseName(s: String): List[String] = {
          val split = s.split(" ")
          if (split.length == 2) List(split(0), split(1))
          else List(s)
        }

        def getInstagram(string: String): Option[String] = {
          if (Pattern.matches(".*\\p{InCyrillic}.*", string)) null
          else Option(string)
        }

        val name = parseName(row(1))
        val instagramOrNotes = getInstagram(row(0))
        Customer(Option(id), name.head, if (name.length == 1) null else Option(name.tail.head),
          row(2), null, null, null, instagramOrNotes, null, Option(if (instagramOrNotes == null) row(0) + " " else "" + (if(row. length >= 15) row(14) else "")) // TODO: Deal with phones !!!!
        )
      }
      def getCustomerAndAddressFromRow(row: Array[String], id: Int, gettingAddress: (Array[String], Int) => Address): (Customer, Address) = {
        (getCustomer(row, id), gettingAddress(row, id))
      }
      def getInsertsForCustomerAndAddresses(t: (Customer, Address)): SQLCommand = {
        generateInsertForCustomer(t._1) + generateInsertForAddress(t._2)
      }
      @tailrec
      def rowsToCommands(lines: Seq[Array[String]], i: Int, acc: Seq[SQLCommand]): Seq[SQLCommand] = lines match {
        case r :: rs => rowsToCommands(
          rs,
          i + 1,
          getInsertsForCustomerAndAddresses(getCustomerAndAddressFromRow(r, i, getAddress)) +: acc)
        case Seq() => acc
      }
      rowsToCommands(lines, startWithID, Seq())
    }

    val linesWithSeparatedAddress = CSVReader.open(new File("/Users/andrewyakovenko/Downloads/First Part.csv"))
      .all()
      .map(_.toArray)
    val linesWithOutSeparatedAddress = CSVReader.open(new File("/Users/andrewyakovenko/Downloads/Second Part.csv"))
      .all()
      .map(_.toArray)
    val firstPart = importRows(linesWithSeparatedAddress, 1, getAddressWithSeparation)
    val secondPart = importRows(linesWithOutSeparatedAddress, linesWithSeparatedAddress.length, getAddressWithOutSeparation)
    val printer = new PrintWriter("customer.sql")
    firstPart.foreach(printer.write)
    secondPart.foreach(printer.write)
    printer.close()
  }

  private val customersTableProperties = Map(
    "id" -> "id",
    "firstName" -> "first_name",
    "lastName" -> "last_name",
    "phone" -> "phone",
    "phoneNote" -> "phone_note",
    "phone2" -> "phone2",
    "phoneNote2" -> "phone2_note",
    "instagram" -> "instagram",
    "preferences" -> "preferences",
    "notes" -> "notes"
  )
  private val addressTableProperties = Map(
    "id" -> "id", "customerID" -> "customer_id",
    "city" -> "city",
    "residentialComplex" -> "residential_complex",
    "address" -> "address",
    "entrance" -> "entrance",
    "floor" -> "floor",
    "flat" -> "flat",
    "notesForCourier" -> "note_for_courier"

  )

  def generateInsertForCustomer(customer: Customer): SQLCommand = {
    "insert into customers " + generateInsertingProperties(getProperties(List(
      "id",
      "firstName", "lastName",
      "phone", "phoneNote",
      "phone2", "phoneNote2",
      "instagram",
      "preferences", "notes"), customer.productIterator), customersTableProperties) + generateInsertingVariables(getVariables(customer.productIterator)) + "; \n"
  }

  def generateInsertForAddress(address: Address): SQLCommand = {
   val x = "insert into addresses " + generateInsertingProperties(getProperties(List("id",
      "customerID", "city", "residentialComplex",
      "address", "entrance",
      "floor", "flat",
      "notesForCourier"), address.productIterator), addressTableProperties) + generateInsertingVariables(getVariables(address.productIterator)) + "; \n"
    x
  }
}

object CustomerImporter extends CustomerImporter with App {
  importCustomersCSV()
}


/*
 * TODO: Counter For Customer IDs
 *
 */