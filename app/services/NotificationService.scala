package services

import actors.NotificationActor
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.adapter.ClassicActorSystemOps

import javax.inject.{Inject, Singleton}

// ═══════════════════════════════════════════════════════════════════════
//  NotificationService — wraps NotificationActor behind plain methods.
//
//  Analogy — think "phone contact":
//    - val notifActor   = a saved phone number in your contacts
//                         (saved ONCE when the service is created)
//    - def matchCreated = the action of DIALING that number
//                         (called MANY times — once per POST to /matches)
//
//  You don't "call the number" — you USE it to dial.
//  Same here: the def USES the val, doesn't call it.
//
//  @Singleton tells Guice: make ONLY ONE copy of this service for the
//  whole app → ONE actor spawned, ONE phone contact, reused forever.
// ═══════════════════════════════════════════════════════════════════════
@Singleton
class NotificationService @Inject() (system: ActorSystem) {

  // "Save the number" — runs ONCE when the service is constructed.
  // system.spawn creates the actor and returns a reference (phone number).
  // We save it in notifActor so we can dial it later.
  private val notifActor: ActorRef[NotificationActor.Command] =
    system.spawn(NotificationActor(), "notification-actor")

  // "Dial the saved number" — runs EACH TIME the controller calls this.
  // Reads the saved notifActor and uses `!` (tell) to send a MatchCreated letter.
  // Fire-and-forget — no reply, so return type is Unit.
  //
  // Example:
  //   notificationService.matchCreated(42, "Wembley")
  //     → inside, we do:  notifActor ! MatchCreated(42, "Wembley")
  //     → actor logs: "Match 42 created at Wembley"
  def matchCreated(matchId: Long, venue: String): Unit =
    notifActor ! NotificationActor.MatchCreated(matchId, venue)
}
