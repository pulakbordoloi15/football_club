package controllers

import models.PlayerError.{DuplicatePlayer, EmptyName, EmptyNationality, EmptyPosition, InvalidAge}
import models.{Player, PlayerError}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import repositories.{ClubRepository, PlayerRepository}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlayerController @Inject()(cc: ControllerComponents, playerRepo: PlayerRepository, clubRepo: ClubRepository)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private lazy val logger: Logger = Logger(this.getClass)


  //  private val store = mutable.Map[Long, Player]()
  //  private var nextId = 1L

  def getAll(): Action[AnyContent] = Action.async {
    logger.warn("Getting all players")
    playerRepo.findAll().map(players => Ok(Json.toJson(players)))
  }

  //Future.successful is only needed outside .map(), like when you return early before calling the repository:
  def getById(id: Long): Action[AnyContent] = Action.async {
    playerRepo.findById(id).map {
      case Some(player) => Ok(Json.toJson(player))
      case None => NotFound(Json.obj("error" -> s"Player with id $id not found"))
    }
  }

  def getByClubId(clubId: Long): Action[AnyContent] = Action.async {
    playerRepo.findByClubId(clubId).map(clubs => Ok(Json.toJson(clubs)))
  }

  def create(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Player] match {
      case JsError(errors) => Future.successful(BadRequest(JsError.toJson(errors)))
      case JsSuccess(player, _) =>
        validatePlayer(player) match {
          case Left(error) =>
            Future.successful(BadRequest(Json.obj("error" -> errorToMessage(error))))
          case Right(validPlayer) =>
            playerRepo.findByNameAndClubId(validPlayer.name, validPlayer.clubId).flatMap {
              case Some(_) =>
                Future.successful(BadRequest(Json.obj("error" ->
                  errorToMessage(DuplicatePlayer(validPlayer.name, validPlayer.clubId)))))
              case None => clubRepo.findById(validPlayer.clubId).flatMap {
                case Some(_) => playerRepo.create(validPlayer).map(created => Created(Json.toJson(created)))
                case None => Future.successful(BadRequest(Json.obj("error" -> s"Club ${validPlayer.clubId} not found")))
              }
            }
        }
    }
  }

  def update(id: Long): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Player] match {
      case JsError(errors) => Future.successful(BadRequest(JsError.toJson(errors)))
      case JsSuccess(player, _) =>
        validatePlayer(player) match {
          case Left(error) =>
            Future.successful(BadRequest(Json.obj("error" -> errorToMessage(error))))
          case Right(validPlayer) =>
            playerRepo.findByNameExcludingId(validPlayer.name, id, validPlayer.clubId).flatMap {
              case Some(_) =>
                Future.successful(BadRequest(Json.obj("error" ->
                  errorToMessage(DuplicatePlayer(validPlayer.name, validPlayer.clubId)))))
              case None =>
                playerRepo.update(id, player).map {
                  case Some(updated) => Ok(Json.toJson(updated))
                  case None => NotFound(Json.obj("error" -> s"Player with id $id not found"))
                }
            }
        }
    }
  }

  def delete(id: Long): Action[AnyContent] = Action.async {
    playerRepo.delete(id).map {
      case true => NoContent
      case false => NotFound(Json.obj("error" -> s"Player $id not found"))
    }
  }

  def validatePlayer(player: Player): Either[PlayerError, Player] = {
    if (player.name.trim.isEmpty) Left(EmptyName)
    else if (player.age < 0 || player.age > 100) Left(InvalidAge(player.age))
    else if (player.position.trim.isEmpty) Left(EmptyPosition)
    else if (player.nationality.trim.isEmpty) Left(EmptyNationality)
    else
      Right(player)
  }

  def errorToMessage(error: PlayerError): String = error match {
    case EmptyName => "Name cannot be empty"
    case InvalidAge(age) => s"Age $age is invalid (must be 0-100)"
    case EmptyPosition => "Position cannot be empty"
    case EmptyNationality => "Nationality cannot be empty"
    case DuplicatePlayer(n, c) => s"Player '$n' already exists in club $c"
  }
}
