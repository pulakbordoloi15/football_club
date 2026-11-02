package actors

import models.MatchStatus
import models.MatchStatus.InProgress
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, OFormat}
import repositories.MatchRepository

import scala.concurrent.duration.DurationInt


//For simulating match
object MatchSimulatorActor {


  private val log = LoggerFactory.getLogger("actors.MatchSimulatorActor")

  // 1. STATE — the data the actor remembers
  case class LiveState(
                        minute: Int,
                        homeScore: Int,
                        awayScore: Int,
                        status: MatchStatus
                      )    // STATE (a struct)

  object LiveState {
    implicit val format: OFormat[LiveState] = Json.format[LiveState]
  }

  //Two designs
  //One state---> what to show for if asked
  //Second(2 Behaviors)--->Operations..like tick, start, getSate


  // 2. COMMANDS — the messages others send to the actor
  //               (Tick, Start, GetState — operations)
  sealed trait Command

  case object Tick extends Command   //message

  case object Start extends Command  //message

  case class GetState(replyTo: ActorRef[LiveState]) extends Command   //message


  def apply(matchId: Long,matchRepository: MatchRepository): Behavior[Command] = {
    scheduled(matchId,matchRepository)
  }

  private def scheduled(matchId: Long,matchRepository: MatchRepository): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Start =>
          log.info(s"[Match $matchId] starting")
          Behaviors.withTimers { timers =>
            timers.startTimerAtFixedRate(Tick, 1.second)
            inProgress(matchId, LiveState(0, 0, 0, MatchStatus.InProgress),matchRepository)
          }

        case GetState(replyTo) =>
          replyTo ! LiveState(0, 0, 0, MatchStatus.Scheduled)
          Behaviors.same

        case Tick =>
          Behaviors.same
      }
    }
  }

  private def inProgress(matchId: Long, state: LiveState,matchRepository: MatchRepository): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Tick =>
          val newMinute = state.minute + 1
          val homeGoal = scala.util.Random.nextInt(100) < 2
          val awayGoal = scala.util.Random.nextInt(100) < 2
          val next= state.copy(
            minute = newMinute,
            homeScore = state.homeScore  + (if (homeGoal) 1 else 0),
            awayScore = state.awayScore + (if (awayGoal) 1 else 0),
            status =state.status
          )

          if( newMinute >= 90){
            implicit val ec = context.executionContext   // ← get from Pekko
              val finishedState= next.copy(status = MatchStatus.Finished)
            log.info(s"Match $matchId finished")
            matchRepository.updateScore(matchId,finishedState.homeScore,finishedState.awayScore,finishedState.status).foreach{
              case true=> log.info(s"Match $matchId score filled")
              case false=>log.info(s"Match $matchId not ended")
            }
              finished(matchId, next.copy(status = MatchStatus.Finished))
          }
          else{
            inProgress(matchId, next,matchRepository)
          }

        case GetState(replyTo)=>
          replyTo ! state
          Behaviors.same

        case Start =>
          Behaviors.same
      }

    }
  }

  private def finished(matchId: Long, state: LiveState): Behavior[Command] = {
    Behaviors.receive { (context,message)=>
      message match {
        case GetState(replyTo) =>
          replyTo ! state
          Behaviors.same

        case _=>
          Behaviors.same
      }
    }
  }


}
