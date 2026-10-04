package models

import org.apache.pekko.http.scaladsl.model.DateTime
import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class Match(id:Option[Long],homeClubId:Long, awayClubId:Long,scheduledAt:Instant,venue:String,status:MatchStatus,homeScore:Option[Int],awayScore:Option[Int])


object Match {
  implicit val format: OFormat[Match] = Json.format[Match]
}