package com.utamars.api

import java.util.UUID

import akka.http.scaladsl.model.{StatusCodes, FormData}
import akka.http.scaladsl.server.Route
import cats.data.Xor
import com.utamars.dataaccess._
import spec.ServiceSpec

class ClockInOutServiceSpec extends ServiceSpec {

  val service = new ClockInOutService()

  before {
    initDataBase
  }

  after {
    clearCache
  }

  "Clock in/out service" should {
    val request = requestWithCredentials(assistantBobAcc.username, assistantBobAcc.passwd, Route.seal(service.route)) _
    val uuid = UUID.randomUUID().toString

    "response with 200 on a successful clock in" in {
      scalacache.sync.cachingWithTTL(uuid)(2.seconds)("")  // simulate registering the uuid
      request(Post("/clock-in", FormData("uuid" -> uuid, "computerid" -> "ERB 103"))) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "response with 200 on a successful clock out" in {
      scalacache.sync.cachingWithTTL(uuid)(2.seconds)("")  // simulate registering the uuid
      request(Post("/clock-out", FormData("uuid" -> uuid, "computerid" -> "ERB 103"))) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "save a clock in record into the database after clocking in" in {
      scalacache.sync.cachingWithTTL(uuid)(2.seconds)("")  // simulate registering the uuid
      request(Post("/clock-in", FormData("uuid" -> uuid, "computerid" -> "ERB 103"))) ~> check {
        transaction {
          ClockInOutRecord.findByEmployeeId(assistantBob.employeeId) match {
            case Xor.Right(records) =>
              records.head.clockingIn shouldEqual true
              records.head.employeeId shouldEqual assistantBob.employeeId
              records.head.computerId shouldEqual "ERB 103"
            case Xor.Left(err)      => fail(err.toString)
          }
        }
      }
    }

    "save a clock out record into the database after clocking out" in {
      scalacache.sync.cachingWithTTL(uuid)(2.seconds)("")  // simulate registering the uuid
      request(Post("/clock-out", FormData("uuid" -> uuid, "computerid" -> "ERB 103"))) ~> check {
        transaction {
          ClockInOutRecord.findByEmployeeId(assistantBob.employeeId) match {
            case Xor.Right(records) =>
              records.head.clockingIn shouldEqual false
              records.head.employeeId shouldEqual assistantBob.employeeId
              records.head.computerId shouldEqual "ERB 103"
            case Xor.Left(err)      => fail(err.toString)
          }
        }
      }
    }

    "update the assistant currentlyClockedIn column after clocking in/out" in {
      scalacache.sync.cachingWithTTL(uuid)(2.seconds)("")  // simulate registering the uuid

      request(Post("/clock-in", FormData("uuid" -> uuid, "computerid" -> "ERB 103"))) ~> check {
        transaction {
          Assistant.findByUsername(assistantBob.username) match {
            case Xor.Right(assistant) => assistant.currentlyClockedIn shouldEqual true
            case Xor.Left(err)        => fail(err.toString)
          }
        }
      }

      request(Post("/clock-out", FormData("uuid" -> uuid, "computerid" -> "ERB 103"))) ~> check {
        transaction {
          Assistant.findByUsername(assistantBob.username) match {
            case Xor.Right(assistant) => assistant.currentlyClockedIn shouldEqual false
            case Xor.Left(err)        => fail(err.toString)
          }
        }
      }
    }

    "response with 410 if either the UUID was not registered or it has been expired" in {
      // NOTE: did not simulate registering the uuid
      request(Post("/clock-in", FormData("uuid" -> uuid, "computerid" -> "ERB 103"))) ~> check {
        status shouldEqual StatusCodes.Gone
      }

      scalacache.sync.cachingWithTTL(uuid)(500.millis)("")  // simulate registering the uuid
      Thread.sleep(100)
      request(Post("/clock-in", FormData("uuid" -> uuid, "computerid" -> "ERB 103"))) ~> check {
        status shouldEqual StatusCodes.OK
      }

      Thread.sleep(501) // wait for uuid to expirer
      request(Post("/clock-in", FormData("uuid" -> uuid, "computerid" -> "ERB 103"))) ~> check {
        status shouldEqual StatusCodes.Gone
      }
    }
  }

}
