package models

sealed trait PlayerError

object PlayerError {
  case object EmptyName extends PlayerError

  case class InvalidAge(age: Int) extends PlayerError

  case object EmptyPosition extends PlayerError

  case object EmptyNationality extends PlayerError

  case class DuplicatePlayer(name: String, clubId: Long) extends PlayerError
}
