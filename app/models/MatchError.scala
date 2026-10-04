package models

sealed trait MatchError

object MatchError {
  case class SameClubs(ClubId:Long) extends MatchError
  case class HomeClubNotFound(homeClubId:Long) extends MatchError
  case class AwayClubNotFound(awayClubId:Long) extends MatchError
  case object PastScheduledTime extends MatchError
  case object EmptyVenue extends MatchError
  case object ScoreBeforeStart extends MatchError
}