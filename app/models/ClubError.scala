package models

sealed trait ClubError

object  ClubError {

  case object EmptyName extends ClubError

  case class InvalidFoundedYear(age: Int) extends ClubError

  case object EmptyCity extends ClubError

  case class DuplicateClub(name: String, clubId: Long) extends ClubError
}