package actors

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

// ═══════════════════════════════════════════════════════════════════════
//  NotificationActor — a mail-room assistant for "something happened" events.
//
//  Think of this actor like a secretary at a desk with an inbox.
//  Someone drops a slip saying "Match 42 was created at Wembley".
//  The secretary reads it, writes it in the logbook, done.
//  Nobody waits for a reply — fire-and-forget.
//
//  Real-world uses of this pattern:
//    - Send emails after a user signs up (actor doesn't block the HTTP response)
//    - Push notifications
//    - Audit logs
//    - Cache invalidation
//    - Analytics events
//
//  Why an actor vs a plain function? Scalability + future-proofing:
//    If one day we need retries, rate-limiting, or a queue — the actor
//    is the right shape to grow into. For NOW, it's basically a log line.
// ═══════════════════════════════════════════════════════════════════════
object NotificationActor {

  // ─── Message protocol ─────────────────────────────────────────────
  // sealed trait = "the complete menu of slips this actor accepts".
  // Compiler blocks any other type → safer than classic (untyped) actors.
  sealed trait Command

  // Only one kind of slip right now. We can add more later (MatchUpdated,
  // MatchDeleted) — all extending Command.
  case class MatchCreated(matchId: Long, venue: String) extends Command

  // ─── The behavior ────────────────────────────────────────────────
  // Behaviors.setup runs ONCE when the actor is first created.
  // It gives us a `context` (logger, child creation, etc.).
  //
  // Behaviors.receiveMessage handles each incoming message.
  //
  // Behaviors.same at the end means "stay in this same behavior for the
  // next message" — our actor is stateless, so we never change behavior.
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val log = context.log

    Behaviors.receiveMessage {
      case MatchCreated(id, venue) =>
        log.info(s"[NotificationActor] Match $id created at $venue")
        Behaviors.same
    }
  }
}
