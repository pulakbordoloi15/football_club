package repositories

import io.getquill.{MysqlJdbcContext, SnakeCase}
import models.Player
import play.api.Logger
import play.api.db.Database

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlayerRepository @Inject()(db:Database)(implicit ec:ExecutionContext) {
  private lazy val logger: Logger = Logger(this.getClass)


  lazy val ctx= new MysqlJdbcContext(SnakeCase,db.dataSource.asInstanceOf[javax.sql.DataSource with java.io.Closeable])
  import ctx._

  private val playersTable= quote(querySchema[Player]("players"))
  //querySchema means Player case class maps to players table.
  //quote{} wraps the mapping as a recipe which says "read from the players table"

  def findAll():Future[Seq[Player]]=Future{
    logger.warn("Inside Repo findAll")
    ctx.run(playersTable)
    //ctx.run() --this is where the  it is generated + executed
  }

  def findById(id:Long): Future[Option[Player]]= Future{
    ctx.run(playersTable.filter(_.id.contains(lift(id)))).headOption
  }

  def findByClubId(clubId:Long):Future[Seq[Player]]= Future{
    ctx.run(playersTable.filter(_.clubId== lift(clubId)))
  }

  def findByNameAndClubId(name:String,clubId:Long):Future[Option[Player]]=Future{
    ctx.run(
      playersTable.filter(p=>p.name==lift(name) && p.clubId==lift(clubId))
    ).headOption
  }

  def create(player:Player):Future[Player]= Future{
    val generatedId=ctx.run(
      playersTable.insert(
        _.name-> lift(player.name),
        _.position->lift(player.position),
        _.nationality->lift(player.nationality),
        _.age->lift(player.age),
        _.clubId-> lift(player.clubId)
      ).returningGenerated(_.id)  // get the auto-generated id back
    )
    player.copy(id=generatedId)
  }

  def findByNameExcludingId(name:String,excludeId:Long,clubId:Long) :Future[Option[Player]]=Future{
    ctx.run(playersTable.filter(p=> p.name ==lift(name) && (p.clubId==lift(clubId) && (!p.id.contains(lift(excludeId)))))).headOption
  }

  def update(id:Long, player:Player):Future[Option[Player]]= Future{
    val count=ctx.run(
      playersTable.filter(_.id.contains(lift(id))).update(
        _.name-> lift(player.name),
        _.position->lift(player.position),
        _.nationality->lift(player.nationality),
        _.age->lift(player.age),
        _.clubId-> lift(player.clubId)
      )
    )
    if (count>0) Some(player.copy(id=Some(id))) else None
  }


  def delete(id:Long):Future[Boolean]=Future{
    val count =ctx.run(playersTable.filter(_.id.contains(lift(id))).delete)
    count>0
  }

}
