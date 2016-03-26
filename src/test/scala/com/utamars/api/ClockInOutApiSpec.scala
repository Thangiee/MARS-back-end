package com.utamars.api

import akka.http.scaladsl.model.{FormData, StatusCodes}
import akka.http.scaladsl.server.Route
import cats.data.Xor
import com.github.nscala_time.time.Imports._
import com.utamars.ApiSpec
import com.utamars.dataaccess._

import scala.concurrent.Await
import scalacache._
import scalacache.guava.GuavaCache

class ClockInOutApiSpec extends ApiSpec {
  implicit val scalaCache = ScalaCache(GuavaCache())
  val api = new ClockInOutApi()

  override def beforeAll(): Unit = {
    initDataBase()
  }

  before {
    initDataBaseData()
  }

  after {
    Await.ready(scalacache.removeAll(), 5.seconds)
    deleteAllDataBaseData()
  }

  "Clock in/out service" should {

    "response with 200 on a successful clock in" in {
      asstRequest(Post("/records/clock-in", FormData("computer_id" -> "ERB 103"))) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "response with 200 on a successful clock out" in {
      asstRequest(Post("/records/clock-out", FormData("computer_id" -> "ERB 103"))) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "save a clock in record into the database after clocking in" in {
      asstRequest(Post("/records/clock-in", FormData("computer_id" -> "ERB 103"))) ~> check {
        Await.result(ClockInOutRecord.findMostRecent(asstBob.netId).value, 1.minute) match {
          case Xor.Right(record) =>
            record.netId shouldEqual asstBob.netId
            record.outTime shouldEqual None
            record.inComputerId shouldEqual Some("ERB 103")
          case Xor.Left(err) => fail(err.toString)
        }
      }
    }

    "save a clock out record into the database after clocking out" in {
      asstRequest(Post("/records/clock-in", FormData("computer_id" -> "ERB 103"))) ~> check {
        asstRequest(Post("/records/clock-out", FormData("computer_id" -> "ERB 103"))) ~> check {
          Await.result(ClockInOutRecord.findMostRecent(asstBob.netId).value, 1.minute) match {
            case Xor.Right(record) =>
              record.netId shouldEqual asstBob.netId
              record.outTime shouldBe defined
              record.inComputerId shouldEqual Some("ERB 103")
            case Xor.Left(err) => fail(err.toString)
          }
        }
      }
    }

    "response with 409 if an assistant try to clock in but is already clocked in" in {
      asstRequest(Post("/records/clock-in", FormData("computer_id" -> "ERB 103"))) ~> check {
        asstRequest(Post("/records/clock-in", FormData("computer_id" -> "ERB 103"))) ~> check {
          status shouldEqual StatusCodes.Conflict
        }

        // now clock out then in
        asstRequest(Post("/records/clock-out", FormData("computer_id" -> "ERB 103"))) ~> check {
          asstRequest(Post("/records/clock-in", FormData("computer_id" -> "ERB 103"))) ~> check {
            status shouldEqual StatusCodes.OK
          }
        }
      }
    }
  }

}
