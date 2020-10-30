package services.importer

import scala.annotation.tailrec
import java.io.File
import org.apache.poi.ss.usermodel.{DataFormatter, WorkbookFactory}
import collection.JavaConversions._

class Importer {
  type SQLFragment = String
  type SQLCommand = String
  type Line = List[String]
  type Lines = Array[Line]
  val endOfSQLCommand = "; \n"
  def startOfSQLCommand(tableName: String) = s"insert into $tableName"

  def getListInBraces(list: List[String], withVarcharBraces: Boolean = false): SQLFragment = {
    def getString(v: String): String = if (withVarcharBraces) s"'$v'" else s"$v"
    @tailrec
    def listToString(list: List[String], acc: String, last: Boolean = false): String =  list match {
      case List() => acc
      case s :: ss =>
        if (last || list.length == 1) listToString(ss, acc + getString(s))
        else {
          if (ss.isEmpty || ss.tail.isEmpty) listToString(ss, acc + getString(s) + ", ", last = true)
          else listToString(ss, acc + getString(s) + ", ")
        }
    }

    if(list.isEmpty) "()"
    else "(" + listToString(list, "") + ")"
  }

  def generateInsertingProperties(p: List[String], mapProperties: Map[String, String]): SQLFragment = getListInBraces(p.map(mapProperties(_)))

  def generateInsertingVariables(v: List[String]): SQLFragment = " values " + getListInBraces(v, withVarcharBraces = true)

  def getAllCVSsInDirectory(directory: File): List[File] = {
    directory.listFiles(_.isFile).filter(_.getName.endsWith(".csv")).toList
  }

  def getAllXLSXsInDirectory(directory: File): List[File] = {
    directory.listFiles(_.isFile).filter(f => f.getName.endsWith(".xlsx") || f.getName.endsWith(".xls")).toList
  }

  def xlsxToLines(xlsx: File): Lines = {
    val workbook = WorkbookFactory.create(xlsx)
    val sheet = workbook.getSheetAt(0)
    val formatter = new DataFormatter()
    sheet.map{ row =>
      row.cellIterator().map(cell => formatter.formatCellValue(cell)).toList
    }.toArray
  }

  def getVariables(iterator: Iterator[Any]): List[String]= iterator.toList.filter(_ != null).map {
    case Some(v) => v.toString
    case a => a.toString
  }
  def getProperties(allProperties: List[String], factProperties: Iterator[Any]): List[String] = allProperties.zip(factProperties.toList).filter(_._2 != null).map(_._1)
}