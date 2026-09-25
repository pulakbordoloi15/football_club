# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start the dev server (auto-reloads on Scala source changes)
sbt -mem 4000 -Dquill.macro.log=false run

# Compile only
sbt compile

# Run tests
sbt test

# Run a single test class
sbt "testOnly controllers.ClubControllerSpec"
```

The `-Dquill.macro.log=false` flag silences Quill's compile-time SQL output. `-mem 4000` prevents sbt OOM on large recompiles.

Play dev mode initialises the DB **lazily** — evolutions and the first DB connection happen on the **first HTTP request**, not at startup.

## Stack

- **Scala 2.13.18** + **Play Framework 3.0.11** (Pekko-based)
- **Quill 4.8** (`MysqlJdbcContext` + `SnakeCase`) — compile-time SQL generation
- **MySQL 8** via HikariCP connection pool
- **Play Evolutions** (`autoApply=true`) — schema migrations in `conf/evolutions/default/`
- **Guice** dependency injection
- CSRF disabled for the API (`play.filters.disabled += "play.filters.csrf.CSRFFilter"`)

## Architecture

```
app/
  controllers/   — HTTP layer (ClubController, PlayerController)
  models/        — Case classes + JSON typeclasses in companion objects
  repositories/  — DB layer (one repo per table, Quill queries)
conf/
  routes                        — URL → controller method mapping
  application.conf              — DB config, evolutions autoApply
  evolutions/default/1.sql      — schema migrations (Ups/Downs)
```

### Request flow

```
HTTP request
  → routes (conf/routes)
  → Controller (Action.async → Future[Result])
  → Repository (Future { ctx.run(...) })  ← blocking JDBC, shifted to thread pool
  → MySQL via HikariCP
```

### Key conventions

**Repositories** mirror tables, not response shapes. One repo per table (`ClubRepository` → `clubs`, `PlayerRepository` → `players`). All repo methods return `Future[T]` wrapping a blocking `ctx.run(...)` call in `Future { }`.

**Models** split into two kinds:
- *Entities* (`Club`, `Player`) — map to DB tables, use `OFormat` (both `Reads` + `Writes`) because they appear in both request bodies and responses.
- *DTOs* (`ClubWithPlayers`) — response-only shapes, use `Writes` only. Built by composing entities in the controller, no repository.

**Quill `SnakeCase`** maps Scala field names automatically: `foundedYear` → `founded_year`, `clubId` → `club_id`. The mapping is compile-time — if field names don't match, queries silently return wrong data rather than erroring.

**JSON implicits** live in each model's companion object so they're auto-visible everywhere without imports.

### Dependency injection

Controllers receive repos via `@Inject` constructor params. Repos receive Play's `Database` and an implicit `ExecutionContext`. The Quill context is `lazy val ctx` inside each repo — created on first use, not at startup.

### FK validation — two layers

`Player.clubId` FK is enforced at both:
1. DB level — `CONSTRAINT fk_players_club FOREIGN KEY (club_id) REFERENCES clubs(id)` in `1.sql`
2. App level — `PlayerController.create` calls `clubRepo.findById` before inserting

### MySQL local dev note

If connecting to MySQL 8 with `caching_sha2_password`, the JDBC URL needs:
```
?allowPublicKeyRetrieval=true&useSSL=false
```
Without this, HikariCP times out after 30s with a "Public Key Retrieval is not allowed" root cause.
