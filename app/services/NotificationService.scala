package services

import actors.NotificationActor
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.adapter.ClassicActorSystemOps

import javax.inject.{Inject, Singleton}

@Singleton
class NotificationService @Inject() (system: ActorSystem) {

  private val notifActor:ActorRef[NotificationActor.Command]=
    system.spawn(NotificationActor(),"notification-actor")

  def matchCreated(matchId:Long,venue:String):Unit=
    notifActor ! NotificationActor.MatchCreated(matchId, venue)

}
