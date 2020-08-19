package services

import java.util.Date
import java.text.SimpleDateFormat
import models.Address
import play.api.data.Form
import play.api.mvc.{Call, MessagesRequestHeader}
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

  def convertMenuTypeToString(menuType: Int): String = {
    val translator = Map(
      1 -> "Класичне",
      2 -> "Лайт",
      3 -> "Сніданок",
      4 -> "Суп",
      5 -> "Десерт",
      6 -> "Промо"
    )
    translator.getOrElse(menuType, throw new RuntimeException(s"Error can't Map $menuType"))
  }

  def menuTypeToTittleForOfferCreatePage(menuType: Int): String = {
    val menuTypeToTitle = Map (
      1 -> "Встановлення Класичного Меню",
      2 -> "Встановлення Лайт Меню",
      3 -> "Встановлення Сніданок Меню",
      4 -> "Встановлення Суп Меню",
      5 -> "Встановлення Десерт Меню",
      6 -> "Встановлення Промо Меню"
    )
    menuTypeToTitle(menuType)
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
    def createInputForForm(form: Form[T], withError: Boolean, keyInForm: String, label: String, layoutClass: String = "", inputLimit: Int, errorMessage: String): SHTML = {
      if (withError) {
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
    formatter.format(d)
  }

  /**
   * Used to Format java.util.Date to String that can be used in a form
   * With out Formatting Play framework will Return Error in form
  * */
  def formattingDateForForm(d: Date): String = formatter.format(d)

  def  getNextSundayFromCornetWeek: String = {
    val c = Calendar.getInstance
    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    formattingDateForForm(c.getTime)
  }

  def getNextSundayFromGivenDate(date: Date): String = {
    val c = Calendar.getInstance()
    c.setTime(date)
    c.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY)
    c.add(Calendar.DATE, 7)
    formattingDateForForm(c.getTime)
  }

  def getLastSundayFromGivenDate(date: Date): String = {
    val c = Calendar.getInstance
    c.setTime(date)
    c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    c.add(Calendar.DATE, -7)
    formattingDateForForm(c.getTime)
  }

  def getNextMondayFromCornetWeek: String = {
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
     * repeatWithIndex will run even list is empty so we need math for "field"
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
    def getString(s: Any): String = {
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

  def getStandardSearchDiv(formCall: Option[Call], dropdownHead: Option[String], sideButtonHref: Option[Call], withSideJS: Boolean): Html = {
    val formMethod = if (formCall.isDefined) formCall.head.method else ""
    val formAction = if (formCall.isDefined) formCall.head.url else ""
    val sideButtonAction = if (sideButtonHref.isDefined) sideButtonHref.head.url else ""
    val dropdownDiv = if (dropdownHead.isDefined) getSHtmlInDiv("searchMenu", "dropdown-menu", SHTML(s"<span class=${inDQ("dropdown-header")}>${dropdownHead.getOrElse("")}</span>")) else SHTML("")

    val mainContent: SHTML = {
      SHTML(s"<form action=${inDQ(formAction)} method=${inDQ(formMethod)} class=${inDQ("colFirst form-inline md-form mr-auto mb-4")}>") +
        getSHtmlInDiv(layoutClass = "input-group", content = {
          SHTML(s"<input id=${inDQ("search")} name=${inDQ("search")} type=${inDQ("search")} class=${inDQ("form-control mr-sm-4 dropdown-toggle")} data-toggle=${inDQ("dropdown")} aria-haspopup=${inDQ("true")} aria-expanded=${inDQ("false")}>") +
            dropdownDiv +
            SHTML(s"<input type=${if (withSideJS) inDQ("button") else inDQ("submit")} id=${inDQ("searchSubmit")} value=${inDQ("Пошук")} class=${inDQ("btn btn-primary btn-rounded btn-sm my-0")}>")
        }) +
        SHTML("</form>") +
        getSHtmlInDiv(layoutClass = "col", content = SHTML("")) +
        getSHtmlInDiv(layoutClass = "colLast", content = SHTML(s"<a class= ${inDQ(" colLast btn btn-success")} href=${inDQ(sideButtonAction)} >Додати</a>"))
    }

    getSHtmlInDiv("searchDIV", "row container-fluid", mainContent).toPlayHTML
  }

  /**
   * Have to use with s"..." because Play will fall with Compilation error of ( ')' expected but string literal found. )
   * Even if Idea Compiler doesn't see any problem
   **/
  private def inDQ(string: String): String = {
    val doubleQuote = "\""
    doubleQuote + string + doubleQuote
  }

  /**
   * additionalSettings are for anything that not common use HTML attributes but without need to write one more method
   * should be like getSHtmlInDiv("7", "btn btn-primary", SHTML(...), "aria-labelledby="dropdownMenuButton"")
   **/
  private def getSHtmlInDiv(id: String = "", layoutClass: String = "", content: SHTML, additionalSettings: String = ""): SHTML = {
    SHTML(s"<div id=${inDQ(id)} class= ${inDQ(layoutClass)} $additionalSettings>") + content + SHTML("</div>")
  }

}
