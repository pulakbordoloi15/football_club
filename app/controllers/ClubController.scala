package controllers

import models.ClubError.{DuplicateClub, EmptyCity, EmptyName, InvalidFoundedYear}
import models.{Club, ClubError, ClubWithPlayers}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import repositories.{ClubRepository, PlayerRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClubController @Inject()(cc: ControllerComponents, clubRepo: ClubRepository, playerRepo: PlayerRepository)(implicit ec: ExecutionContext) extends AbstractController(cc) {


  //  def getAll(): Action[AnyContent] = Action {
  //    Ok(Json.toJson(store.values.toSeq))
  //  }
  //Big change — everything becomes Action.async because repository returns Future:

  def getAll(): Action[AnyContent] = Action.async {
    //Ok(Json.toJson(store.values.toSeq))
    clubRepo.findAll().map(clubs => Ok(Json.toJson(clubs)))
  }

//  What .map() does
//.map() transforms the value inside a Future:

//    Future[X].map(x => Y)  →  Future[Y]
//  Whatever you return inside .map() gets wrapped in a new Future.

  def getById(id: Long): Action[AnyContent] = Action.async {
    clubRepo.findById(id).map {
      //.map unwraps the Future. Inside .map, we now have Option[Club].
      case Some(club) => Ok(Json.toJson(club))
      //Some(club) → serialize club to JSON → wrap in 200 Ok
      case None => NotFound(Json.obj("error" -> s"Club with id $id not found"))
    }
  }

//  What .flatMap() does
//.flatMap() is the same as .map() but it flattens nested Futures:

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Club] match {
      case JsError(errors) => Future.successful(BadRequest(JsError.toJson(errors)))
      case JsSuccess(club, _) =>
        validateClub(club) match {
          case Left(error)=>
            Future.successful(BadRequest(Json.obj("error"-> errorToMessage(error)))
          case Right(club)=>
            clubRepo.create(club).map(created => Created(Json.toJson(created)))
        }
    }
  }

  def update(id: Long): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Club] match {
      case JsError(errors) => Future.successful(BadRequest(JsError.toJson(errors)))
      case JsSuccess(club,_) => clubRepo.update(id, club).map {
        case Some(updated) => Ok(Json.toJson(updated))
        case None => NotFound(Json.obj("error" -> s"Club with id $id not found"))
      }
    }
  }

  def delete(id: Long): Action[AnyContent] = Action.async {
    clubRepo.delete(id).map{
      case true  => NoContent
      case false => NotFound(Json.obj("error" -> s"Club $id not found"))
    }
  }

  def getWithPlayers(id:Long):Action[AnyContent]= Action.async {
    clubRepo.findById(id).flatMap{
      case None =>Future.successful(NotFound(Json.obj("error"->s"Club with id $id not found")))
      case Some(club)=>
        playerRepo.findByClubId(id).map{
          players=> Ok(Json.toJson(ClubWithPlayers(club,players)))
        }
    }
  }

  def validateClub(club:Club):Either[ClubError,Club]= {
    if(club.name.trim.isEmpty) Left(EmptyName)
    else if (club.foundedYear < 0) Left(InvalidFoundedYear(club.foundedYear))
    else if (club.city.trim.isEmpty) Left(EmptyCity)
    else
      Right(club)
  }


  def errorToMessage(error: ClubError): String = error match {
    case EmptyName => "Name cannot be empty"
    case InvalidFoundedYear(year) => s"Age $year is invalid (must be 0-100)"
    case EmptyCity => "Position cannot be empty"
    case DuplicateClub(n, c) => s"Player '$n' already exists in club $c"
  }


}
