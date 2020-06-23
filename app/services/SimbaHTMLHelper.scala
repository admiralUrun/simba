package services

import java.util.Date
import java.text.SimpleDateFormat

import models.{Address, Customer, PlayOrderForEditAndCreate}
import play.api.data.Form
import play.api.mvc.MessagesRequestHeader
import play.twirl.api.{Html, JavaScript}
import java.util.Calendar

object SimbaHTMLHelper {
  private val formatter = new SimpleDateFormat("yyyy-MM-dd")

  def formatString(o: Option[String], n: Option[String]): String = {
    o.map(p => p + n.map(n => s"($n)").getOrElse("")).getOrElse("")
  }

  def getStringOrDash(s: Option[String]): String = s.getOrElse("-")

  def getFlash()(implicit request: MessagesRequestHeader): Html = {
    def getFlash(option: Option[String], alertClass: String): SHTML = {
      if (option.nonEmpty) option.map { message =>
        SHTML(s"<div class=${stringInDoubleQuote(s"alert-$alertClass")}><strong>$message</strong></div>")
      }.head
      else SHTML("")
    }

    (getFlash(request.flash.get("success"), "success") += getFlash(request.flash.get("error"), "error")).toPlayHTML
  }

  def createTextInputWithoutForm(inputId: String, label: String, layoutClass: String = ""): Html = {
    val input: SHTML = {
      SHTML(s"<div class= ${stringInDoubleQuote(s"form-group $layoutClass")} >") +=
        SHTML(s"<label class=${stringInDoubleQuote("control-label")} for= ${stringInDoubleQuote(inputId)}> $label </label>") +=
        SHTML(s"<input type=${stringInDoubleQuote("text")} class=${stringInDoubleQuote("form-control")} id=${stringInDoubleQuote(inputId)}>") +=
        SHTML("</div>")
    }
    input.toPlayHTML
  }

  def createTextArea(idName: String, label: String, layoutClass: String = "", rowsInTextArea: Int = 3, inTextArea: Option[String] = Option("")): Html = {
    val textArea: SHTML = {
      SHTML(s"<div class=${stringInDoubleQuote(s"form-group $layoutClass")}>") +=
        SHTML(s"<label for=${stringInDoubleQuote(idName)}>$label</label>") +=
        SHTML(
          s"""<textarea class=${stringInDoubleQuote("form-control")}
              id=${stringInDoubleQuote(idName)} name=${stringInDoubleQuote(idName)}
              rows=${stringInDoubleQuote(rowsInTextArea.toString)}
             >${inTextArea.getOrElse("")}</textarea>""") +=
        SHTML("</div>")
    }
    textArea.toPlayHTML
  }

  def formattingDateForDisplay(d: Date): String = {
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

  def formattingDateForForm(d: Date): String = formatter.format(d)

  def getNextSunday: String = {
    val c = Calendar.getInstance
    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    formattingDateForForm(c.getTime)
  }

  def getNextMonday: String = {
    val c = Calendar.getInstance
    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    formattingDateForForm(c.getTime)
  }

  def showError[T](keyInForm: String, form: Form[T]): Boolean = {
    form.error(keyInForm) match {
      case None => false
      case Some(_) => true
    }
  }

  def getCheckBoxValue[T](keyInForm: String, form: Form[T]): Boolean = {
    form(keyInForm).value match {
      case None => false
      case Some(value) => value.toBoolean
    }
  }

  def getValueFromForm[T](keyInForm: String, form: Form[T]): Option[String] = form(keyInForm).value

  def tableHeaders(heads: List[String], darkHead: Boolean = false): Html = {
    val h = if (darkHead) SHTML("<thead class='thead-dark'>") else SHTML("<thead>")
    (h += SHTML(heads.zipWithIndex.map { case (header, i) =>
      s"<th class=${stringInDoubleQuote(s"col${i + 1} header")}>$header</th>"
    }) += SHTML("</thead>")).toPlayHTML
  }

  def stringToAddress(addressString: String): Address = {
    val a = addressString.split(",")

    def searchInStringFor(search: String): Option[String] = {
      val o = a.find(_.contains(search))
      if (o.isDefined) Option(o.head.replace(search, ""))
      else null
    }

    Address(null, null, a(0).replace("(city)", ""), searchInStringFor("(residentialComplex)"), a(1).replace("(address)", ""),
      searchInStringFor("(entrance)"),
      searchInStringFor("(floor)"),
      searchInStringFor("(flat)"),
      searchInStringFor("(notes)"))
  }

  def createDropDown[T](title: String, content: Seq[T], generateDropItemFromContent: T => String): Html = {
    (SHTML(
      s"""
      <div class= "dropdown">
        <button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            $title
        </button>
        <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
        """) +=
      SHTML(content.map(generateDropItemFromContent)) +=
      SHTML(
        """
         </div>
      </div>
        """)).toPlayHTML
  }

  def getStandardDropItem(title: String, jSOnclick: JavaScript, htmlID: String = ""): String = {
    s"<a id=${stringInDoubleQuote(htmlID)} class=${stringInDoubleQuote("dropdown-item")} href=${stringInDoubleQuote("#")} onclick=${stringInDoubleQuote(jSOnclick.body)} >$title</a>"
  }

  private def stringInDoubleQuote(string: String): String = {
    val doubleQuote = "\""
    doubleQuote + string + doubleQuote
  }

}
