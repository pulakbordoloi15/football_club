# Scala / Play Learning Notes


## Future and Action.async

The problem without async

Play handles many requests at the same time. If every request blocks waiting for the database:

Request 1 → waiting for DB... (thread blocked)
Request 2 → waiting for DB... (thread blocked)
Request 3 → waiting for DB... (thread blocked)
// all threads busy → new requests have to wait

The server runs out of threads and slows down.

Future — do it later, don't block now

A Future is a box that says "I don't have the value yet, but I will".

val result: Future[Seq[Club]] = clubRepo.findAll()
// doesn't wait for DB
// returns immediately with a "promise" of data
// thread is free to handle other requests

When the DB responds later, the Future completes with the value inside.

## .map
.map() — what to do when Future completes

clubRepo.findAll().map(clubs => Ok(Json.toJson(clubs)))
//                 ^^^
//                 "when Future completes, take clubs and return Ok response"

You never touch the value directly — you tell Scala what to do with it when it arrives.

## Action.async
Action.async — tells Play to expect a Future

// Action — expects Result right now
def getAll = Action {
Ok("hello")  // immediate value
}

// Action.async — expects Future[Result]
def getAll = Action.async {
clubRepo.findAll().map(clubs => Ok(Json.toJson(clubs)))  // future value
}

Without Action.async, Play doesn't know how to handle a Future[Result] — it expects a plain Result.

Future.successful — already have the value, no async needed

Future.successful(BadRequest(...))

Sometimes you already know the result (e.g. JSON was invalid) — no DB call needed. But since the method must return Future[Result], you wrap it with Future.successful to satisfy the type.

---
Full picture

Request comes in
↓
Action.async      → tells Play "wait for a Future"
↓
Future { }        → DB query starts on background thread
↓
main thread free  → handles other requests meanwhile
↓
DB responds       → Future completes
↓
.map()            → transforms result into HTTP response
↓
Play sends response back to client


## .flatMap

.flatMap() = .map() + flatten. Use when your function returns another Future.

Rule of thumb:
  - function returns a plain value → .map
  - function returns a Future      → .flatMap

Future[X].map(x => Y)             → Future[Y]
Future[X].flatMap(x => Future[Y]) → Future[Y]        (flattened)
Future[X].map(x => Future[Y])     → Future[Future[Y]]  ❌ nested, won't compile in Action.async

Example — getWithPlayers uses BOTH:

clubRepo.findById(id).flatMap {                       // outer branches return Future[Result]
  case None       => Future.successful(NotFound(...))
  case Some(club) =>
    playerRepo.findByClubId(id).map { players =>       // inner returns plain Result → .map
      Ok(Json.toJson(ClubWithPlayers(club, players)))
    }
}

Every time you write .map or .flatMap, ask:
  "What does my function return — a plain value or a Future?"


## for-comprehension over Futures

Sugar for .flatMap + .map. Each `<-` desugars to a .flatMap, except the last which is a .map.

Ugly nested flatMap:
  futureA.flatMap { a => futureB.map { b => a + b } }

Cleaner as for:
  for {
    a <- futureA
    b <- futureB
  } yield a + b

Both compile to the same bytecode.

Sequential vs parallel — key insight:
A Future starts running the moment it's CONSTRUCTED, not when <- unwraps it.

  // SEQUENTIAL — futureB starts after futureA finishes
  for {
    a <- runQueryA()   // constructed inside for
    b <- runQueryB(a)  // constructed inside for, can't start until a is known
  } yield (a, b)

  // PARALLEL — both start immediately
  val futA = runQueryA()   // constructed BEFORE the for
  val futB = runQueryB()   // constructed BEFORE the for
  for {
    a <- futA
    b <- futB
  } yield (a, b)

When Option sits inside Future (like findById returning Future[Option[Club]]),
for-comprehension gets awkward — prefer .flatMap { case None => ...; case Some => ... }.


## Quill — compile-time SQL

Quill converts Scala code to SQL at COMPILE TIME using macros.
That's the key difference from Slick (runtime) or JDBC (you write SQL as strings).

Three pieces:
  ctx           — MysqlJdbcContext, the JDBC-aware runner with connection pool
  quote { ... } — macro that captures your query as an AST at compile time
  ctx.run(q)    — turns the AST into a SQL string, executes it via JDBC

  import ctx._  — brings run/quote/query/querySchema/lift + implicit encoders into scope

quote captures, run executes:
  val clubsTable = quote(querySchema[Club]("clubs"))  // just a description
  ctx.run(clubsTable)                                 // NOW SQL is generated + run

lift — bridges runtime values into compile-time SQL:
  ctx.run(clubsTable.filter(_.id.contains(lift(id))))
  //                                     ^^^^^^^
  //                                     "insert this runtime value as a JDBC parameter"

MysqlJdbcContext.run is BLOCKING — wrap in Future { } to shift to background thread.


## JSON typeclasses — Reads / Writes / Format / OFormat

Think of translators between Scala and JSON.

  Writes[T]  = English → French dictionary  (send OUT only)
  Reads[T]   = French → English dictionary  (receive IN only)
  Format[T]  = bilingual                    (both directions)
  OFormat[T] = bilingual + JSON is always { } object (case classes)

Direction is REQUEST vs RESPONSE — NOT GET vs POST.
A single POST endpoint uses BOTH — Reads on the body, Writes on the response.

Decision table:
  Received in a request?  Sent in a response?  → Use
       no                     yes               Writes[T]
       yes                    no                Reads[T]
       yes                    yes               OFormat[T] (case class) or Format[T]
       no                     no                nothing — don't define one

Family tree:
  OFormat[T] extends Format[T] extends (Writes[T] + Reads[T])

So OFormat is the "everything" typeclass — fits any slot.


## The three macros

  Json.reads[T]   → Reads[T]
  Json.writes[T]  → Writes[T]   (case class → OWrites[T])
  Json.format[T]  → Format[T]   (case class → OFormat[T])

Pick the macro that matches the typeclass name.

Constraint: macros need every field of the case class to ALREADY have its own
Reads / Writes / Format in scope (typically in the field's companion object).


## Companion object convention

Put implicit typeclass instances in the type's companion object:

  object Club {
    implicit val format: OFormat[Club] = Json.format[Club]
  }

Why? Implicit values in a type's companion object are automatically in scope
wherever that type is used. No `import` needed.

  // In the controller, this just works — no import for the format:
  Ok(Json.toJson(club))


## Json.toJson vs Json.obj

Json.toJson(x)     — you HAVE a Scala value; convert IT to JSON
                     uses implicit Writes[T]
                     good for: case classes, Seq, Option

Json.obj(k -> v)   — you're BUILDING a small JSON object by hand
                     good for: errors, status envelopes, ad-hoc shapes

Decision:
  "Do I already have a Scala object for this?"
    yes → Json.toJson
    no  → Json.obj

From my own controller:
  Ok(Json.toJson(clubs))                                    // Seq[Club] → toJson
  Ok(Json.toJson(club))                                     // Club → toJson
  NotFound(Json.obj("error" -> s"Club $id not found"))      // ad-hoc error → obj
  Ok(Json.toJson(ClubWithPlayers(club, players)))           // DTO → toJson

Combined pattern — obj containing toJson:
  Json.obj(
    "success" -> true,
    "data"    -> Json.toJson(club)     // or just "data" -> club (auto-conversion)
  )

Also:
  Json.arr(a, b)      — build JsArray inline
  Json.parse(str)     — String → JsValue
  Json.stringify(js)  — JsValue → String (Play does this automatically on Ok(...))


## Entity vs DTO — repos mirror TABLES, not response shapes

Two kinds of models:
  Entity — maps to a DB table       (Club, Player)   → needs a Repository
  DTO    — shapes an API response   (ClubWithPlayers) → NO Repository

There is no `club_with_players` table in MySQL. Nothing to SELECT from.
DTOs are BUILT in the controller by combining entities.

Rule of thumb:
  Repository = one per table, returns raw entities only
  Controller = composes entities into response DTOs

If you'd added ClubWithPlayersRepository, every new endpoint would need a
new repo — ClubWithCoachesRepo, ClubWithStadiumRepo, etc. Explodes fast.

Exception: if composition is a DB-level JOIN for performance, it can live
INSIDE the existing repo (e.g. ClubRepository.findWithPlayers()) — not a
new repo.


## Action[T] — T is the request body type

Action[AnyContent]   — default, body may be anything (GET/DELETE)
Action[JsValue]      — with parse.json, body pre-parsed as JSON
Action[Map[...]]     — with parse.form, form-encoded body
Action[Multipart]    — with parse.multipartFormData, file uploads

Type parameter always matches what the body parser produces.

  def getAll(): Action[AnyContent] = Action.async { ... }
  def create: Action[JsValue] = Action.async(parse.json) { request => ... }

If parse.json fails (bad JSON in body) → Play returns 400 automatically.
Your handler never runs.


## Scala naming convention

  Type / class / object / trait  → UpperCamelCase   (Club, ClubWithPlayers, Json)
  Value / method / variable      → lowerCamelCase   (club, players, findById)

That's why `club` (the value) and `Club` (the type) can coexist and mean
totally different things. Get the case wrong → compiler error like:
  "not found: value clubWithPlayers"


## Compiling ≠ correct

The compiler checks TYPES, not BEHAVIOR.

Code can compile, return 200, and still be wrong if the response shape
doesn't match what the endpoint's URL/name/docs promise.

Example — my getWithPlayers first draft returned only players. Compiled fine,
returned 200, but broke the endpoint's contract.

Rule: if you create a DTO for an endpoint, that endpoint MUST actually return
that DTO. Otherwise delete the DTO.


## Troubleshooting

MySQL 8 default auth = caching_sha2_password.
  Local dev JDBC URL usually needs:
    ?allowPublicKeyRetrieval=true&useSSL=false
  NEVER use these in production.

HikariCP timeout = the POOL couldn't open a connection, not that a query
was slow. Real cause is usually deeper in the stack trace (auth failure,
wrong host, wrong password).

Play returns 404 (NOT 405) for wrong-method-on-existing-URL.
  If Postman shows 404 + HTML page and Chrome works fine, first check
  Postman's method dropdown — Chrome always uses GET for typed URLs.

URLs in Play routes are CASE-SENSITIVE:
  /getWithPlayers/1 ≠ /getwithplayers/1
