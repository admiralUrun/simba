package services

import play.api.libs.json.Writes

object SimbaAlias {
  type ID = Int
  type  JSONWrites[T] = Writes[T]
}
