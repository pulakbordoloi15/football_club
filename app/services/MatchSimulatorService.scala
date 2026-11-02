package services

import actors.MatchSimulatorActor.{Command, LiveState, Start}
import actors.{MatchSimulatorActor, NotificationActor}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.util.Timeout
import repositories.MatchRepository

import javax.inject.{Inject, Singleton}
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

@Singleton
class MatchSimulatorService @Inject()(system: ActorSystem,matchRepository: MatchRepository)(implicit ec: ExecutionContext) {

  //State-thread safe
  private val actors = TrieMap[Long, ActorRef[Command]]()

  //Implicit for ask
  private implicit val timeout: Timeout = 3.seconds
  private implicit val scheduler: org.apache.pekko.actor.typed.Scheduler = system.toTyped.scheduler


  def startMatch(matchId: Long): Unit = {

    if (!actors.contains(matchId)) {
      val newRef = system.spawn(MatchSimulatorActor(matchId,matchRepository ), s"match-$matchId")
      newRef ! Start
      actors.put(matchId, newRef)
    }
  }


  def getLive(matchId: Long): Future[Option[LiveState]] = {
    actors.get(matchId) match {
      case Some(ref) =>
        ref.ask(replyTo => MatchSimulatorActor.GetState(replyTo)).map(Some(_))
      case None =>
        Future.successful(None)
    }
  }


}
