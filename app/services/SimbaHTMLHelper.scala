package services
import java.util.Date

import models.{Customer, PlayOrderForEditAndCreate}
import play.api.mvc.MessagesRequestHeader
import play.twirl.api.Html
object SimbaHTMLHelper {
  type HTMLLines = String
  private val b ="\""
  def formatString(o: Option[String], n: Option[String]): String = {
    o.map(p => p + n.map(n => s"($n)").getOrElse("")).getOrElse("")
  }

  def getFlash(request: MessagesRequestHeader): Html = {
    def getFlash(option: Option[String]):String = {
      if (option.nonEmpty) option.map { message =>
        s"<div class=$b alert-success $b><strong>$message</strong></div>"
      }.head
      else ""
    }
    Html(getFlash(request.flash.get("success")) +"\n" +
      getFlash(request.flash.get("error")))
  }

  def formattingDateForDisplay(d:Date):String = {
    val monthsTranslate = Map(
      "Jan" -> "Січень",
      "Feb" -> "Лютий",
      "Mar" -> "Березень",
      "Apr" -> "Квітень",
      "May" -> "Травень",
      "Jun" -> "Червень",
      "Jul" -> "Липень",
      "Aug" -> "Серпень",
      "Sep" -> "Вересень",
      "Oct" -> "Жовтень",
      "Nov" -> "Листопад",
      "Dec" -> "Грудень",
    )
    val weekDayTranslate = Map(
      "Mon" -> "Понеділок",
      "Tue" -> "Вівторок",
      "Wed" -> "Середа",
      "Thu" -> "Четверг",
      "Fri" -> "П'ятниця",
      "Sat" -> "Суббота",
      "Sun" -> "Неділя"
    )
    val dateArray = d.toString.split(" ")
    weekDayTranslate(dateArray(0)) + " " + dateArray(2) + " " + monthsTranslate(dateArray(1)) + " " + dateArray(5)
  }
  def formattingDateForForm(d:Date): String = {
    import java.text.SimpleDateFormat
    val formatter = new SimpleDateFormat("yyyy-MM-dd")
    formatter.format(d)
  }

  def tableHeaders(heads: List[String]): Html = {
    htmlBuilder(heads.zipWithIndex.map{ case (header, i) =>
      s"<th class=$b col${i + 1} header$b>$header</th>"
    })
  }

  def insertNotes(a: Option[Any]): String = {
    a match {
      case None => ""
      case Some(PlayOrderForEditAndCreate(_, _, _, _, _, _, _, _, _, _, note)) => note.getOrElse("")
      case Some(Customer(_, _, _, _, _, _,_ , _, _, _, _, _, _, _, notes)) => notes.getOrElse("")
      case _ => ""
    }
  }

  private def htmlBuilder(s: List[HTMLLines]): Html = {
    Html(s.mkString("\n"))
  }

}
