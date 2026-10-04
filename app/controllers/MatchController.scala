package controllers

import models.{Match, MatchError}
import models.MatchError.{AwayClubNotFound, EmptyVenue, HomeClubNotFound, PastScheduledTime, SameClubs, ScoreBeforeStart}
import models.MatchStatus.Scheduled
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import repositories.{ClubRepository, MatchRepository}

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class MatchController @Inject()(cc: ControllerComponents, matchRepo: MatchRepository, clubRepo: ClubRepository)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  //  - getAll() — simple, no validation
  //    - getById(id)
  //  - getByClubId(clubId) — uses your findByClubId
  //  - create() — JSON parse → validateMatch → check both clubs exist → insert
  //  - update(id) — same as create + find excluding self
  //  - delete(id) — simple


  def getAll(): Action[AnyContent] = Action.async {
    val allMatches = matchRepo.findAll();
    allMatches.map { matches => Ok(Json.toJson(matches)) }
    //  In Play Framework, Json.toJson(matches) converts your Scala objects into a standardized JSON format
    //  that web browsers, mobile apps, or frontend frameworks (like React or Vue) can understand.
  }

  def getById(id: Long): Action[AnyContent] = Action.async {
    matchRepo.findById(id).map {
      case Some(m) => Ok(Json.toJson(m))
      case None => NotFound(Json.obj("errors" -> s"Matches Not found for id $id"))
    }
  }

  def getByClubId(clubId: Long): Action[AnyContent] = Action.async {
    matchRepo.findByClubId(clubId).map(m => Ok(Json.toJson(m)))
  }

  def create(): Action[JsValue] = Action.async(parse.json) { request =>
    //Action[T] describes the body INPUT, not the return value
    //The return is always Result (or Future[Result])
    //You extract the JSON from the request using request.body.asJson and validate it using .validate[T]

    // validate[T] = JsSuccess(parsed) | JsError(errors) — same shape as Either, just for JSON
    request.body.validate[Match] match {
      case JsError(errors) =>
        Future.successful(BadRequest(JsError.toJson(errors)))
      case JsSuccess(m, _) =>
        validateMatch(m) match {
          case Left(error) => Future.successful(BadRequest(Json.obj("errors" -> errorToMessage(error))))

          case Right(validMatch) =>
            for {
              mayBeHomeClub <- clubRepo.findById(m.homeClubId)
              mayBeAwayClub <- clubRepo.findById(m.awayClubId)
              result <- (mayBeHomeClub, mayBeAwayClub) match {
                case (_, None) => Future.successful(BadRequest(Json.obj("errors" -> errorToMessage(AwayClubNotFound(m.awayClubId)))))
                case (None, _) => Future.successful(BadRequest(Json.obj("errors" -> errorToMessage(HomeClubNotFound(m.homeClubId)))))
                case (Some(_), Some(_)) => matchRepo.create(validMatch).map { created => Created(Json.toJson(created)) }
              }
            } yield result
        }
    }
  }
  //Sometime itis used to show eerors as json.obj sometimes json.toJson

  def update(id: Long): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Match] match {
      case JsError(errors) =>
        Future.successful(BadRequest(JsError.toJson(errors)))
      case JsSuccess(m, _) =>
        validateMatch(m) match {
          case Left(error) => Future.successful(BadRequest(Json.obj("errors" -> errorToMessage(error))))
          case Right(validMatch) =>
            for {
              mayBeHomeClub <- clubRepo.findById(m.homeClubId)
              mayBeAwayClub <- clubRepo.findById(m.awayClubId)
              result <- (mayBeHomeClub, mayBeAwayClub) match {
                case (_, None) => Future.successful(BadRequest(Json.obj("errors" -> errorToMessage(AwayClubNotFound(m.awayClubId)))))
                case (None, _) => Future.successful(BadRequest(Json.obj("errors" -> errorToMessage(HomeClubNotFound(m.homeClubId)))))
                case (Some(_), Some(_)) => matchRepo.update(id, validMatch).map {
                  case Some(updated) => Ok(Json.toJson(updated))
                  case None => NotFound(Json.obj("errors" -> s"Match with $id not found"))
                }

              }
            } yield result
        }
    }
  }


  def delete(id: Long): Action[AnyContent] = Action.async {
    matchRepo.delete(id).map {
      case true => NoContent
      case false => NotFound(Json.obj("errors" -> s"The match with id $id is not present "))
    }
  }

  def validateMatch(m: Match): Either[MatchError, Match] = {
    if (m.homeClubId == m.awayClubId) Left(SameClubs(m.homeClubId))
    else if (m.scheduledAt.isBefore(Instant.now())) Left(PastScheduledTime)
    else if (m.venue.trim.isEmpty) Left(EmptyVenue)
    else if (m.status == Scheduled && (m.homeScore.isDefined || m.awayScore.isDefined)) Left(ScoreBeforeStart)

    else
      Right(m)

  }

  def errorToMessage(error: MatchError): String = error match {
    case SameClubs(id) => s"Home and away cannot be the same club (id $id)"
    case HomeClubNotFound(id) => s"Home club with id $id not found"
    case AwayClubNotFound(id) => s"Away club with id $id not found"
    case PastScheduledTime => "Scheduled time must be in the future"
    case EmptyVenue => "Venue cannot be empty"
    case ScoreBeforeStart => "Cannot set score before match starts"
  }

}
