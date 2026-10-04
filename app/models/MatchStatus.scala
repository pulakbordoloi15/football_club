package models

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}


sealed trait MatchStatus

object MatchStatus {
  case object Scheduled extends MatchStatus
  case object InProgress extends MatchStatus
  case object Finished extends  MatchStatus

  implicit val format:Format[MatchStatus]= new Format[MatchStatus]{


    // Custom JSON Format for a sealed trait.
    //
    // Why we need it:
    //   Json.format[Match] auto-generates JSON for simple fields (Long, String, Instant, etc.)
    //   but can't handle sealed traits — it doesn't know how to pick between Scheduled/InProgress/Finished.
    //   So we tell it manually: serialize as a plain string ("Scheduled"), parse the same string back.
    //
    // Why in the companion object:
    //   Scala's implicit search automatically looks in the companion of a type.
    //   Placing it here means any code using MatchStatus gets this Format in scope — no import needed.
    //
    // reads  : JSON string → MatchStatus (REST API input / DB column read)
    // writes : MatchStatus → JSON string (REST API output / DB column write)

    // JSON → MatchStatus (how to read)
    def reads(json: JsValue): JsResult[MatchStatus] = json.validate[String].flatMap {
      case "Scheduled"  => JsSuccess(Scheduled)
      case "InProgress" => JsSuccess(InProgress)
      case "Finished"   => JsSuccess(Finished)
      case other        => JsError(s"Unknown MatchStatus: $other")
    }

    // MatchStatus → JSON (how to write)
    def writes(status: MatchStatus): JsValue = JsString(status match {
      case Scheduled  => "Scheduled"
      case InProgress => "InProgress"
      case Finished   => "Finished"
    })

  }
}
