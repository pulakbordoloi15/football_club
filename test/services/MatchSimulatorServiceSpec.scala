package services

import models.MatchStatus
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.{await, defaultAwaitTimeout}

// ═══════════════════════════════════════════════════════════════════════
//  MatchSimulatorServiceSpec
//
//  Integration-style tests for MatchSimulatorService.
//  Uses GuiceOneAppPerSuite — same pattern as ClubRepositorySpec.
//  Play spins up a mini app, Guice wires up the real service + actor system.
//
//  We DON'T hit the DB in these tests — the service only writes when a
//  match reaches minute 90, and we never wait that long.
//
//  Match IDs used here (9999, 10001, 10002) don't need to exist in DB.
//  The service just spawns in-memory actors keyed by matchId.
// ═══════════════════════════════════════════════════════════════════════
class MatchSimulatorServiceSpec extends PlaySpec with GuiceOneAppPerSuite {

  // Grab the real service from Play's DI — Guice provides ActorSystem,
  // MatchRepository, ExecutionContext, all wired automatically.
  val service: MatchSimulatorService =
    app.injector.instanceOf[MatchSimulatorService]

  "MatchSimulatorService" should {

    "return None for a match that hasn't been started" in {
      // No actor exists for id 9999 → service should return None, not error.
      val result = await(service.getLive(9999L))
      result mustBe None
    }

    "return Some state after startMatch is called" in {
      val matchId = 10001L

      service.startMatch(matchId)      // spawns actor + sends Start
      Thread.sleep(1500)               // give the timer time to fire at least once

      val result = await(service.getLive(matchId))
      result mustBe defined
      result.get.status mustBe MatchStatus.InProgress
      result.get.minute must be > 0
    }

    "be idempotent — calling startMatch twice should not throw" in {
      val matchId = 10002L

      service.startMatch(matchId)
      service.startMatch(matchId)      // second call should silently do nothing
                                       // (our `if (!actors.contains(...))` guard)
      Thread.sleep(1200)
      val result = await(service.getLive(matchId))
      result mustBe defined
    }
  }
}
