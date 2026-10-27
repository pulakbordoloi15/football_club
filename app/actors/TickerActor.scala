package actors

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt

object TickerActor {

  // Explicit logger — we control the name so logback's "actors" rule matches.
  // (context.log inside withTimers gets a Pekko internal name — we avoid that.)
  private val log = LoggerFactory.getLogger("actors.TickerActor")

  sealed trait Command

  // the message the actor sends to itself
  case object Tick extends Command

  def apply(): Behavior[Command] = Behaviors.withTimers { timers =>
    // Schedule a Tick to arrive every 5 seconds, forever
    timers.startTimerAtFixedRate(Tick, 5.seconds)

    Behaviors.receiveMessage {
      case Tick =>
        log.info("tick!")
        Behaviors.same
    }
  }
}
