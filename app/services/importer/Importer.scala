package services.importer
import scala.annotation.tailrec


class Importer {
  type SQLFragment = String
  type SQLCommand = String
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
  def generateInsertingProperties(p: List[String], mapProperties: Map[String, String]): SQLFragment = getListInBraces(p.map(mapProperties(_)))
  def generateInsertingVariables(v: List[String]): SQLFragment = " values " + getListInBraces(v, withSQLBraces = true)

}