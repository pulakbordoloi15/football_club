package controllers

import models.Club
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.ClubRepository

class ClubRepositorySpec extends PlaySpec with GuiceOneAppPerSuite{

  val clubRepo:ClubRepository= app.injector.instanceOf[ClubRepository]

  "ClubRepository" should {

    "findAll return a Seq" in {
      noException should be thrownBy await(clubRepo.findAll())

    }
    "create insert a club and returns it with an id" in {
      val club= Club(None,"Chelsea FC", "London", 1857,"Stanford")
      val res=await(clubRepo.create(club))

      res.id mustBe defined
      await(clubRepo.delete(res.id.get))   // ← clean up
    }

    "findById the club" in {
      val club= Club(None,"Real Madrid", "Madrid",1866,"Bernebeu")
      val created= await(clubRepo.create(club))
      val res= await(clubRepo.findById(created.id.get))
      res mustBe defined
      res.get.name mustBe "Real Madrid"
      await(clubRepo.delete(created.id.get))   // ← clean up

    }

    "findById returns None when club does not exist" in {
      val res = await(clubRepo.findById(99999L))
      res mustBe empty
    }


  }

}
