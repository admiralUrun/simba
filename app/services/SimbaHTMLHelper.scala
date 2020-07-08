package services

import java.util.Date
import java.text.SimpleDateFormat
import models.Address
import play.api.data.Form
import play.api.mvc.MessagesRequestHeader
import play.twirl.api.{Html, JavaScript}
import java.util.Calendar
import views.html.helper._

object SimbaHTMLHelper {
  private val formatter = new SimpleDateFormat("yyyy-MM-dd")

  def formatString(o: Option[String], n: Option[String]): String = {
    o.map(p => p + n.map(n => s"($n)").getOrElse("")).getOrElse("")
  }

  def getStringOrDash(s: Option[String]): String = {
    if (s.isEmpty) "-"
    else s.getOrElse("-")
  }

  def getFlash()(implicit request: MessagesRequestHeader): Html = {
    def getFlash(option: Option[String], alertClass: String): SHTML = {
      if (option.nonEmpty) option.map { message =>
        SHTML(s"<div class=${inDQ(s"alert-$alertClass")}><strong>$message</strong></div>")
      }.head
      else SHTML("")
    }

    (getFlash(request.flash.get("success"), "success") + getFlash(request.flash.get("error"), "danger")).toPlayHTML
  }

  def createInputForForm[T](form: Form[T], keyInForm: String, label: String, layoutClass: String = "", inputLimit: Int, errorMessage: String = ""): Html = {
    def createInputForForm(form: Form[T],withError: Boolean, keyInForm: String, label: String, layoutClass: String = "", inputLimit: Int, errorMessage: String ): SHTML = {
      if(withError) {
        getSHtmlInDiv("", layoutClass, {
          getSHtmlInDiv(layoutClass = "form-group " + layoutClass,
            content = {
              SHTML(s"<label class=${inDQ("control-label")} for=${inDQ(keyInForm)}> $label </label>") +
                SHTML(s"<input id=${inDQ(keyInForm)} name=${inDQ(keyInForm)} value=${inDQ(getValueFromForm(keyInForm, form).getOrElse(""))}   maxlength=${inDQ(inputLimit.toString)} type=${inDQ("text")} class=${inDQ("form-control " + "is-invalid")}>") +
              getSHtmlInDiv("", "invalid-feedback", SHTML(errorMessage))
            })
        })
      } else {
        getSHtmlInDiv(layoutClass = "form-group " + layoutClass,
          content = {
            SHTML(s"<label class=${inDQ("control-label")} for=${inDQ(keyInForm)}> $label </label>") +
              SHTML(s"<input id=${inDQ(keyInForm)} name=${inDQ(keyInForm)} value=${inDQ(getValueFromForm(keyInForm, form).getOrElse(""))} maxlength=${inDQ(inputLimit.toString)} type=${inDQ("text")} class=${inDQ("form-control")}>")
          })
      }
    }
    createInputForForm(form, form.error(keyInForm).isDefined, keyInForm, label, layoutClass, inputLimit, errorMessage).toPlayHTML
  }

  def createTextInputWithoutForm(inputId: String, label: String, layoutClass: String = "", inputLimit: Int): Html = {
    getSHtmlInDiv(layoutClass = "form-group " + layoutClass,
      content = {
        SHTML(s"<label class=${inDQ("control-label")} for= ${inDQ(inputId)}> $label </label>") +
          SHTML(s"<input id=${inDQ(inputId)} maxlength=${inDQ(inputLimit.toString)} type=${inDQ("text")} class=${inDQ("form-control")}>")
      }).toPlayHTML
  }

  def createTextArea(idName: String, label: String, layoutClass: String = "", rowsInTextArea: Int = 3, inTextArea: Option[String] = Option("")): Html = {
      getSHtmlInDiv(layoutClass = "form-group " + layoutClass,
        content = {
          SHTML(s"<label for=${inDQ(idName)}>$label</label>") +
            SHTML(
              s"""<textarea class=${inDQ("form-control")}
              id=${inDQ(idName)} name=${inDQ(idName)}
              rows=${inDQ(rowsInTextArea.toString)}
             >${inTextArea.getOrElse("")}</textarea>""")
        }).toPlayHTML
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
    form.error(keyInForm).isDefined
  }

  def getCheckBoxValue[T](keyInForm: String, form: Form[T]): Boolean = {
    form(keyInForm).value match {
      case None => false
      case Some(value) => value.toBoolean
    }
  }

  def getAllArrayInputFromForm[F, A](form: Form[F], keyInForm: String, constrictor: (A, Int) => Html, addonFunctionOnFiled: String => A): Html = {
    /**
      repeatWithIndex will run even list is empty so we need math for "field"
      */
    SHTML(repeatWithIndex(form(keyInForm)) { (field, i) =>
      field.value match {
        case Some(value) => constrictor(addonFunctionOnFiled(value), i)
        case None => Html("")
      }
    }.map(_.body)).toPlayHTML
  }

  def getValueFromForm[T](keyInForm: String, form: Form[T]): Option[String] = form(keyInForm).value

  def tableHeaders(heads: List[String], darkHead: Boolean = false): Html = {
    val h = if (darkHead) SHTML("<thead class='thead-dark'>") else SHTML("<thead>")

    (h + SHTML(heads.zipWithIndex.map { case (header, i) =>
      s"<th class=${inDQ(s"col${i + 1} header")}>$header</th>"
    }) + SHTML("</thead>")).toPlayHTML
  }

  def stringToAddress(addressString: String): Address = {
    val a = addressString.split(",")

    def searchForString(search: String): Option[String] = {
      val o = a.find(_.contains(search))
      if (o.isDefined) Option(o.head.replace(search, ""))
      else None
    }
    def searchForInt(search: String): Option[Int] = {
      val o = a.find(_.contains(search))
      if (o.isDefined) Option(o.head.replace(search, "").toInt)
      else None
    }

    Address(searchForInt("(id)"), searchForInt("(customerID)"), searchForString("(city)").head, searchForString("(residentialComplex)"), searchForString("(address)").head,
      searchForString("(entrance)"),
      searchForString("(floor)"),
      searchForString("(flat)"),
      searchForString("(notes)"))
  }

  def addressToString(a: Address): String = {
    def getString(s:Any): String = {
      s match {
        case Some(value) => value.toString
        case None => ""
        case _ => s.toString
      }
    }
    val s = List("(id)", "(customerID)", "(city)", "(residentialComplex)", "(address)", "(entrance)", "(floor)", "(flat)", "(notes)").zip(a.productIterator.toList).filter(_._2 != None)
      .map { t =>
        getString(t._2) + t._1
      }.mkString(",")
    s
  }

  def createDropDown[T](title: String, content: Seq[T], generateDropItemFromContent: T => String): Html = {
    getSHtmlInDiv(layoutClass = "dropdown", content = {
      SHTML(s"<button class=${inDQ(" btn btn-secondary dropdown-toggle")} type=${inDQ("button")} data-toggle=${inDQ("dropdown")} aria-haspopup=${inDQ("true")} aria-expanded=${inDQ("false")}>$title</button>") +
      getSHtmlInDiv(layoutClass = "dropdown-menu", content = SHTML(content.map(generateDropItemFromContent)), additionalSettings = "aria-labelledby=\"dropdownMenuButton\"")
    }).toPlayHTML
  }

  def getStandardDropItem(title: String, jSOnclick: JavaScript, htmlID: String = ""): String = {
    s"<a id=${inDQ(htmlID)} class=${inDQ("dropdown-item")} href=${inDQ("#")} onclick=${inDQ(jSOnclick.body)} >$title</a>"
  }

  private def inDQ(string: String): String = {
    val doubleQuote = "\""
    doubleQuote + string + doubleQuote
  }

  /**
   * additionalSettings is for anything that not common use HTML attributes but without need to write one more method
   *  should be like getSHtmlInDiv(???, ???, ???, "aria-labelledby="dropdownMenuButton"")
   * */
  private def getSHtmlInDiv(id: String= "", layoutClass: String ="", content: SHTML, additionalSettings: String = ""): SHTML = {
    SHTML(s"<div id=${inDQ(id)} class= ${inDQ(layoutClass)} $additionalSettings>") + content + SHTML("</div>")
  }

}
