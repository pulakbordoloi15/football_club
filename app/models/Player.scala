package models

import play.api.libs.json.{Json, OFormat}

case class Player(id: Option[Long], name: String, position: String, nationality: String, age: Int, clubId: Long)

object Player {
  implicit val format: OFormat[Player] = Json.format[Player]
}
