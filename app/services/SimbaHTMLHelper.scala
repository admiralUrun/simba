package services
import java.util.Date
import java.text.SimpleDateFormat
import models.{Customer, PlayOrderForEditAndCreate}
import play.api.mvc.MessagesRequestHeader
import play.twirl.api.Html

object SimbaHTMLHelper {
  private val b ="\""
  private val formatter = new SimpleDateFormat("yyyy-MM-dd")
  def formatString(o: Option[String], n: Option[String]): String = {
    o.map(p => p + n.map(n => s"($n)").getOrElse("")).getOrElse("")
  }

  def getFlash()(implicit request: MessagesRequestHeader): Html = {
    def getFlash(option: Option[String]): SHTML = {
      if (option.nonEmpty) option.map { message =>
        SHTML(s"<div class=$b alert-success $b><strong>$message</strong></div>")
      }.head
      else SHTML("")
    }
    (getFlash(request.flash.get("success")) += getFlash(request.flash.get("error"))).toPlayHTML
  }

  def formattingDateForDisplay(d:Date): String = {
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
  def formattingDateForForm(d:Date): String = formatter.format(d)
  def getNextSunday: String = {
    import java.util.Calendar
    val c = Calendar.getInstance
    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    formattingDateForForm(c.getTime)
  }

  def tableHeaders(heads: List[String]): Html = {
    SHTML(heads.zipWithIndex.map{ case (header, i) =>
      s"<th class=$b col${i + 1} header$b>$header</th>"
    }).toPlayHTML
  }

  def insertNotes(a: Option[Any]): String = {
    a match {
      case None => ""
      case Some(PlayOrderForEditAndCreate(_, _, _, _, _, _, _, _, _, _, _, _, note)) => note.getOrElse("")
      case Some(Customer(_, _, _, _, _, _,_ , _, _, _, _, _, _, _, notes)) => notes.getOrElse("")
      case _ => ""
    }
  }
}
