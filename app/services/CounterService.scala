package services

import actors.CounterActor
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import org.apache.pekko.util.Timeout

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


@Singleton
class CounterService @Inject()(system:ActorSystem) {

  private val counterActor:ActorRef[CounterActor.Command]=
    system.spawn(CounterActor(), "counter-actor")


  private implicit val timeout:Timeout= 3.seconds
  private implicit val scheduler:Scheduler=system.toTyped.scheduler


  def increment():Unit={
    counterActor ! CounterActor.Increment
  }

  def getCount():Future[Int]=
    counterActor.ask(ref => CounterActor.GetCount(ref))
}
