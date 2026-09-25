package repositories

import io.getquill.{MysqlJdbcContext, SnakeCase}
import models.Club

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.Database

//Only one instance created — shared across the whole app
//@Inject- Play's dependency injection — automatically provides db and ec
//Database -Play's database connection pool
//implicit ec: ExecutionContext-Thread pool — required by every Future
@Singleton
class ClubRepository @Inject()(db: Database)(implicit ec: ExecutionContext) {

  // Quill context — connects to MySQL using Play's connection pool
  // SnakeCase maps foundedYear → founded_year automatically
  //MysqlJdbcContext-Quill's MySQL driver, uses Play's connection pool
  //SnakeCase-automatic naming convention: foundedYear → founded_year, clubName → club_name
  //lazy val-only created when first used, not at app startup
  //import ctx._-brings Quill's run, quote, query etc. into scope
  lazy val ctx= new MysqlJdbcContext(SnakeCase,db.dataSource.asInstanceOf[javax.sql.DataSource with java.io.Closeable])

  import ctx._

  // tells Quill: Club case class maps to "clubs" table in MySQL
  //Quill works by converting Scala code into SQL at compile time using macros.

  //quote(querySchema[Club]("clubs"))
  //Quill will try to match field names as-is — so your Club case class fields must already match the DB column names exactly.
  //  quote(querySchema[Club](
  //    "clubs",
  //    _.clubName  -> "club_name",
  //    _.createdAt -> "created_at"
  //  ))
  private val clubsTable = quote(querySchema[Club]("clubs"))

  // SELECT * FROM clubs
  //Future{}-Runs the block on a background thread
  def findAll(): Future[Seq[Club]] = Future {
    ctx.run(clubsTable)
  }

  // SELECT * FROM clubs WHERE id = ?
  //lift - "bridge between Scala runtime values and Quill's compile-time SQL world"
  //Future { ... } — runs the blocking JDBC call on a background thread, so Play's main thread stays free.
  // ctx.run(...) — Quill runs the SQL. The SQL was generated at compile time, not at runtime, from the Scala code.
  //.headOption — takes first row → Some(Club) if found, None if not.
  def findById(id: Long): Future[Option[Club]] = Future {
    ctx.run(clubsTable.filter(_.id.contains(lift(id)))).headOption
  }

  def create(club: Club): Future[Club] = Future {
    val generatedId = ctx.run(
      clubsTable.insert(
        _.name -> lift(club.name),
        _.city -> lift(club.city),
        _.foundedYear -> lift(club.foundedYear),
        _.stadium -> lift(club.stadium)
      ).returningGenerated(_.id) // get the auto-generated id back
    )
    club.copy(id = generatedId) //return club with the new id
  }

  def update(id: Long, club: Club): Future[Option[Club]] = Future {
    val count = ctx.run(
      clubsTable.filter(_.id.contains(lift(id))).update(
        _.name -> lift(club.name),
        _.city -> lift(club.city),
        _.foundedYear -> lift(club.foundedYear),
        _.stadium -> lift(club.stadium)
      )
    )
    if (count > 0) Some(club.copy(id = Some(id))) else None
  }

  def delete(id: Long): Future[Boolean] = Future {
    val count = ctx.run(clubsTable.filter(_.id.contains(lift(id))).delete)
    count > 0
  }


}
