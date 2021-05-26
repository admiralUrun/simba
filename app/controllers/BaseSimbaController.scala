package controllers
import play.api.mvc._
import org.json4s._
import org.json4s.jackson.parseJson
import org.json4s.DefaultFormats
import play.api.Logger
import play.api.libs.json.{JsObject, JsString}
import zio.{Cause, Task}
import zio.Cause._

import javax.inject.Inject

abstract class BaseSimbaController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
	implicit val formats: DefaultFormats = DefaultFormats

	def extractClassFromJson(key: String)(implicit request: Request[AnyContent]): Task[String] = {
		Task(request.body.asJson.get.asInstanceOf[JsObject].value(key).asInstanceOf[JsString].value)
	}

	def logError(c: Cause[Throwable], l: Logger): Unit = {
		c match {
			case Die(t) => l.error("Dies", t)
			case Fail(t) => l.error("Fail", t)
			case _ => c.failureOption match {
				case Some(t) => l.error("", t)
				case None => l.error("Can't trace Error")
			}
		}
	}

}
