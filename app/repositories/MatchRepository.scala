package repositories

import io.getquill.{MysqlJdbcContext, SnakeCase}
import models.Match
import play.api.db.Database

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class MatchRepository @Inject()(db:Database)(implicit ec:ExecutionContext) {
  //Every Future operation — Future { ... }, .map, .flatMap, for-comprehension —
  // needs an ExecutionContext to actually run.

//  ctx is a Quill context — a bridge between:
//    - Scala code (your type-safe queries written as Scala)
//  - JDBC (the actual MySQL driver that talks to the database)
//
//  Think of it as the translator + waiter. You write Scala. It translates to SQL. It runs it against MySQL. It brings back the result as Scala objects.
lazy val ctx= new MysqlJdbcContext(SnakeCase,db.dataSource.asInstanceOf[javax.sql.DataSource with java.io.Closeable])

  import ctx._
  import models.MatchStatus

  // Quill needs to know how to convert MatchStatus ↔ String for the "status" column.
  // MappedEncoding tells Quill: "when you see a MatchStatus, store it as a String.
  // When you read a String from the DB, convert it back to a MatchStatus."

  implicit val matchStatusEncoder: MappedEncoding[MatchStatus, String] = MappedEncoding {
    case MatchStatus.Scheduled  => "Scheduled"
    case MatchStatus.InProgress => "InProgress"
    case MatchStatus.Finished   => "Finished"
  }

  implicit val matchStatusDecoder: MappedEncoding[String, MatchStatus] = MappedEncoding {
    case "Scheduled"  => MatchStatus.Scheduled
    case "InProgress" => MatchStatus.InProgress
    case "Finished"   => MatchStatus.Finished
    case other        => throw new IllegalArgumentException(s"Unknown MatchStatus in DB: $other")
  }

//  What's happening:
//    - Encoder (MatchStatus → String): used when WRITING (INSERT/UPDATE)
//  - Decoder (String → MatchStatus): used when READING (SELECT)


  private val matchTable=quote(querySchema[Match]("matches"))



  def findAll():  Future[Seq[Match]]=Future{
    ctx.run(matchTable)
  }

  def findById(id: Long): Future[Option[Match]]=Future{
    ctx.run(matchTable.filter(_.id.contains(lift(id)))).headOption
  }
  def findByClubId(clubId: Long):Future[Seq[Match]] =Future{
    ctx.run(matchTable.filter(m=>m.homeClubId==lift(clubId) || m.awayClubId==lift(clubId)))
  }

  // home OR away
  def create(m: Match): Future[Match]=Future{
    val generatedId=ctx.run(
      matchTable.insert(
        _.homeClubId->lift(m.homeClubId),
        _.awayClubId->lift(m.awayClubId),
        _.scheduledAt->lift(m.scheduledAt),
        _.venue->lift(m.venue),
        _.status->lift(m.status),
        _.homeScore->lift(m.homeScore),
        _.awayScore->lift((m.awayScore))
      ).returningGenerated(_.id)
    )
    m.copy(id=generatedId)
  }
  def update(id: Long, m: Match):Future[Option[Match]]=Future {
    val count= ctx.run(
      matchTable.filter(_.id.contains((lift(id)))).update(
        _.homeClubId->lift(m.homeClubId),
        _.awayClubId->lift(m.awayClubId),
        _.scheduledAt->lift(m.scheduledAt),
        _.venue->lift(m.venue),
        _.status->lift(m.status),
        _.homeScore->lift(m.homeScore),
        _.awayScore->lift((m.awayScore))
      ))

    if(count > 0) Some(m.copy(id=Some((id)))) else None
  }
  def delete(id: Long): Future[Boolean]= Future{
    val count = ctx.run(matchTable.filter(_.id.contains(lift(id))).delete)
    count>0
  }
}
