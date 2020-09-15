package services

import play.api.libs.json.Writes
import play.api.mvc.{Action, AnyContent}

object SimbaAlias {
  type ID = Int
  type  JSONWrites[T] = Writes[T]
  type PlayAction = Action[AnyContent]
}
