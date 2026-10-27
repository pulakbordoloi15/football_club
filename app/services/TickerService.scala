package services


import actors.TickerActor
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.adapter.ClassicActorSystemOps

import javax.inject.{Inject, Singleton}


// This service exists only to SPAWN the TickerActor at app startup.
// There's no public API — the actor just ticks forever on its own.
@Singleton
class TickerService @Inject()(system:ActorSystem) {

 private val ticketActor:ActorRef[TickerActor.Command]=
   system.spawn(TickerActor(),"ticker-actor")

}
