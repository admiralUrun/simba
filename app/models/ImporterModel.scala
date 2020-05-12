package models

import java.io.File
import java.io.PrintWriter

import scala.annotation.tailrec
import com.github.tototoshi.csv.CSVReader


class ImporterModel {
  type SQLFragment = String
  type SQLCommand = String

  def importCustomersFromCSV: Unit = {
    def importRows(): Unit = {
      def getCustomerFromRow(row: Array[String], i: Int): Customer = {
        def parseName(s: String): List[String] = {
          val split = s.split(" ")
          if (split.length == 2) List(split(0), split(1))
          else List(s)
        }

        def parseAddress(s: String): Map[String, String] = {
          def getMap(city:String, address: String, flat: String, entrance:String, floor: String): Map[String, String] = {
            Map("city" -> city,
              "address" -> address,
              "flat" -> flat,
              "entrance" -> entrance,
              "floor" -> floor
            )
          }
          val a = s.split(",")

          if(a.length == 4 ) getMap("Київ", a(0), a(1), a(2), a(3))
          else if(a.length == 5 ) getMap(a(0), a(1), a(2), a(3), a(4))
          else if(a.length == 3 ) getMap("Київ",a(0), a(1), a(2), "")
          else Map("address" -> s"Error on $i +/-1")
        }

        def f(name: List[String], addressMap: Map[String, String]): Customer = {
          Customer( id = null,
            firstName = name.head, lastName = if (name.tail.isEmpty) null else Option(name.tail.head),
            phone = row(2).take(13), phoneNote = null, phone2 = null, phoneNote2 = null,
            city = addressMap.getOrElse("city", "Київ"),
            address = addressMap.get("address").head,
            flat = addressMap.get("flat"),
            entrance = addressMap.get("entrance"),
            floor = addressMap.get("floor"),
            instagram = Option(row(0)),
            preferences = null,
            notes = Option(row(7))
          )
        }
        f(parseName(row(1)), parseAddress(row(3)))
      }
      val printer = new PrintWriter("customer.sql")
      lines.zipWithIndex.map( t => getCustomerFromRow(t._1.toArray, t._2)).foreach(c => printer.write(generateInsertForCustomer(c)))
    }
    importRows()
  }
  private val lines = CSVReader.open(new File("Новые Заказы - все клиенты.csv")).all()
  private val customersTableProperties = Map (
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

  def takePropertiesFormCustomer(c: Customer): List[String] = {
    List("id",
      "firstName", "lastName",
      "phone", "phoneNote",
      "phone2", "phoneNote2",
      "city", "address", "flat", "entrance", "floor",
      "instagram",
      "preferences", "notes").zip(c.productIterator.toList).filter(_._2 != null).map(_._1)
  }
  def takeVariablesFormCustomer(c: Customer): List[String] = {
    c.productIterator.toList.filter(_ != null).map(a => a match {
      case Some(v) => v.toString
      case _ => a.toString
    })
  }
  def generateInsertForCustomer(customer:Customer): SQLCommand =  {
    "insert into customers " + generateInsertingProperties(takePropertiesFormCustomer(customer)) + generateInsertingVariables(takeVariablesFormCustomer(customer)) + "; \n"
  }

  def getListInBraces(list: List[String], withSQLBraces: Boolean = false): SQLFragment = {
    def getString(v: String): String = if (withSQLBraces) s"'$v'" else s"$v"
    @tailrec
    def listToString(list: List[String], acc: String, last: Boolean = false): String =  list match {
      case List() => acc
      case s :: ss =>
        if (last) listToString(ss, acc + getString(s))
        else {
          if (ss.tail.isEmpty) listToString(ss, acc + getString(s) + ", ", last = true)
          else listToString(ss, acc + getString(s) + ", ")
        }
    }

    if(list.isEmpty) "()"
    else "(" + listToString(list, "") + ")"
  }
  def generateInsertingProperties(p: List[String]): SQLFragment = getListInBraces(p.map(customersTableProperties(_)))
  def generateInsertingVariables(v: List[String]): SQLFragment = " values " + getListInBraces(v, withSQLBraces = true)

}

object ImporterRun extends ImporterModel with App {
  importCustomersFromCSV
}
