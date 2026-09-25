package models

import play.api.libs.json.{Json, Writes}

case class ClubWithPlayers(club:Club,players:Seq[Player])

object ClubWithPlayers {
  implicit val writes: Writes[ClubWithPlayers]= Json.writes[ClubWithPlayers]
}
