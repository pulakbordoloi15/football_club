package actors

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

// ═══════════════════════════════════════════════════════════════════════
//  CounterActor — a tally-keeper for how many matches have been created.
//
//  Think of this actor like a receptionist at a desk with ONE notepad.
//  The notepad has a number on it (starts at 0).
//
//  Two kinds of slips (messages) can be dropped on their desk:
//    1. Increment  → cross out the number, write one higher.
//    2. GetCount   → read the number, write it on a reply, drop it
//                    in the asker's own mailbox (replyTo).
//
//  No locks needed. The actor processes slips ONE AT A TIME, in order.
// ═══════════════════════════════════════════════════════════════════════
object CounterActor {

  // ─── Message protocol ─────────────────────────────────────────────
  // sealed trait = "the complete menu of slips this actor accepts".
  // The compiler will stop you from sending any OTHER kind of slip.
  sealed trait Command

  // Slip A — "add one to the tally". No data needed, so case object.
  case object Increment extends Command

  // Slip B — "what's the current count? Reply at this address".
  // replyTo = the asker's mailbox (where to drop the reply).
  // ActorRef[Int] = "a mailbox that accepts Int messages".
  case class GetCount(replyTo: ActorRef[Int]) extends Command

  // ─── Starting behavior ───────────────────────────────────────────
  // Pekko calls apply() to create the actor. It starts with count = 0.
  def apply(): Behavior[Command] = counter(0)

  // ─── The behavior itself (recursive) ──────────────────────────────
  // IMPORTANT: no mutable `var`. We pass the current count as a parameter
  // and return a NEW behavior with the next count. Each message produces
  // a new "version" of the actor with updated state.
  //
  //   counter(0)  ─Increment→  counter(1)  ─Increment→  counter(2) ...
  //
  // This is how actors stay thread-safe WITHOUT locks: state only lives
  // inside the behavior, which the actor replaces one message at a time.
  private def counter(count: Int): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {

        // Someone said "add one". Log it, then become a new behavior with count+1.
        case Increment =>
          context.log.info(s"[CounterActor] Increment → ${count + 1}")
          counter(count + 1)   // new behavior = new state

        // Someone asked for the count. Send the current value to their mailbox.
        // We stay at the same count — Behaviors.same means "no state change".
        case GetCount(replyTo) =>
          replyTo ! count       // "!" = fire-and-forget tell
          Behaviors.same
      }
    }
}
