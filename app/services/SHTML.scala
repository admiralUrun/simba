package services
import play.twirl.api.Html

class SHTML(text: String) {

  def toPlayHTML: Html = Html(text)
  def += (shtml: SHTML): SHTML = {
     new SHTML (text + "\n" + shtml.getText)
  }
  private def getText: String = text
}

object SHTML {
  def apply(text: String): SHTML = new SHTML(text)
  def apply(text: Seq[String]): SHTML = new SHTML(text.mkString("\n"))
  def apply(html: Html): SHTML = SHTML(html.body)
}
