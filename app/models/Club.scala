package models

import play.api.libs.json.{Json, OFormat}

//id,name,city,foundedYear,stadium
case class Club(id: Option[Long], name: String, city: String, foundedYear: Int, stadium: String)

object Club {
  implicit val format: OFormat[Club] = Json.format[Club]
}
