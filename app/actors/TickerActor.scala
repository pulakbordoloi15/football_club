package actors

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt

// ═══════════════════════════════════════════════════════════════════════
//  TickerActor — a background heartbeat.
//
//  Think of it as a clock that beeps every 5 seconds on its own.
//  It sends a letter to ITSELF ("Tick!") every 5 seconds — no one
//  else talks to it.
//
//  Real-world use: scheduled background work — cleanup, polling,
//  "every N seconds" tasks.
// ═══════════════════════════════════════════════════════════════════════
object TickerActor {

  // Explicit logger — we control the name so logback's "actors" rule matches.
  // (context.log inside withTimers gets a weird Pekko internal name — we avoid that.)
  private val log = LoggerFactory.getLogger("actors.TickerActor")

  // ─── What mail this actor accepts ─────────────────────────────────
  //
  //   Command = the type of mail this actor accepts (mailbox label)
  //
  //   Tick    = the ONLY letter. The actor sends it to ITSELF
  //             every 5 seconds via a timer.
  //
  // Nothing outside sends mail to this actor — it's self-driven.
  sealed trait Command
  case object Tick extends Command

  // ─── Behavior ─────────────────────────────────────────────────────
  // withTimers gives us a scheduler.
  // startTimerAtFixedRate = "drop a Tick letter in my own mailbox
  //                         every 5 seconds, forever."
  //
  // Then we handle each Tick by logging and staying in the same behavior.
  def apply(): Behavior[Command] = Behaviors.withTimers { timers =>
    timers.startTimerAtFixedRate(Tick, 5.seconds)

    Behaviors.receiveMessage {
      case Tick =>
        log.info("tick!")
        Behaviors.same
    }
  }
}
