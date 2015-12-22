package com.utamars.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import better.files._
import com.utamars.ServiceSpec
import org.jvnet.mock_javamail.Mailbox

class TimeSheetGenApiSpec extends ServiceSpec {

  val service    = TimeSheetGenApi()

  val bobRequest = requestWithCredentials(asstBobAcc.username, asstBobAcc.passwd, Route.seal(service.route)) _
  val bobMailbox = Mailbox.get(asstBob.email)

  val aliceRequest = requestWithCredentials(instAliceAcc.username, instAliceAcc.passwd, Route.seal(service.route)) _
  val aliceMailbox = Mailbox.get(instAlice.email)

  override def beforeAll(): Unit = {
    initDataBase()
  }

  override def afterAll(): Unit = {
    val outDir = config.getString("service.timesheet.dir")
    outDir.toFile.children.foreach(f => f.delete())
  }

  before {
    initDataBaseData()
  }

  after {
    bobMailbox.clear()
    aliceMailbox.clear()
  }

  "Assistant" should {

    "be able to request a (first-half-month) timesheet to be generated and emailed to them" in {
      bobRequest(Get("/time-sheet/first-half-month?year=2015&month=9")) ~> check {
        status shouldEqual StatusCodes.OK
        Thread.sleep(100)
        bobMailbox.size shouldEqual 1
        bobMailbox.get(0).getSubject shouldEqual s"timesheet ${asstBob.lastName} ${asstBob.firstName} 09-15-2015"
        bobMailbox.get(0).getContent should not be null
      }
    }

    "be able to request a (second-half-month) timesheet to be generated and emailed to them" in {
      bobRequest(Get("/time-sheet/second-half-month?year=2015&month=9")) ~> check {
        status shouldEqual StatusCodes.OK
        Thread.sleep(100)
        bobMailbox.size shouldEqual 1
        bobMailbox.get(0).getSubject shouldEqual s"timesheet ${asstBob.lastName} ${asstBob.firstName} 09-30-2015"
        bobMailbox.get(0).getContent should not be null
      }
    }

  }

  "Instructor" should {

    "be able to request an assistant timesheet to be generated and emailed to the instructor" in {
      aliceRequest(Get(s"/time-sheet/${asstBob.netId}/first-half-month?year=2015&month=9")) ~> check {
        status shouldEqual StatusCodes.OK
        Thread.sleep(100)
        aliceMailbox.size shouldEqual 1
        aliceMailbox.get(0).getSubject shouldEqual s"timesheet ${asstBob.lastName} ${asstBob.firstName} 09-15-2015"
        aliceMailbox.get(0).getContent should not be null
      }
    }
  }

  "The system" should {
    "response with 404 if the system can not find the assistant from the instructor request" in {
      aliceRequest(Get(s"/time-sheet/NotExistNetId/first-half-month?year=2015&month=9")) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "response with 400 if the request is made with invalid date" in {
      bobRequest(Get("/time-sheet/first-half-month?year=2015&month=13")) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }
}
