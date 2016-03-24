package com.utamars.api

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, FormData}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route._
import cats.data.XorT
import cats.std.all._
import com.utamars.dataaccess.{Account, Assistant}
import com.utamars.{ExeCtx, ApiSpec}
import com.utamars.api.DAOs.AssistantDAO

import scala.concurrent.Future

class AssistantApiSpec extends ApiSpec {

  val api = AssistantApi()

  override def beforeAll(): Unit = {
    initDataBase()
    initDataBaseData()
  }

  "Instructor user" must {
    "be able to update assistant account info" in {
      instRequest(_ => Post(s"/assistant/${asstBob.netId}", FormData("rate" -> "16.64"))) ~> check {
        instRequest(_ => Get(s"/assistant/${asstBob.netId}")) ~> check {
          responseTo[AssistantDAO].rate should equal(16.64)
        }
      }
    }
  }


  "Assistant user" must {

    "be able to update their account info" in {
      asstRequest(_ => Post("/assistant", FormData("title" -> "new title"))) ~> check {
        asstRequest(_ => Get("/assistant")) ~> check {
          responseTo[AssistantDAO].title should equal("new title")
        }
      }
    }

    "not be able to change their account threshold value" in {
      asstRequest(_ => Post("/assistant", FormData("threshold" -> ".5"))) ~> check(status should equal(Forbidden))
    }

  }

  "Creating a new assistant account with a username and/or a net id that already exist" must {
    "not delete the existing account with that username" in {
      implicit val mock = new MockFacePP {
        override def personCreate(personName: String)(implicit ec: ExeCtx) = XorT.right[Future, HttpResponse, Unit](Future.successful(()))
      }

      def getAsstBob = adminRequest(_ => Get(s"/assistant/${asstBob.netId}"))

      getAsstBob ~> check {
        status should equal(OK)
        createAcc(asstBobAcc, asstBob) ~> seal(api.route) ~> check { // attempt to create duplicate account
          getAsstBob ~> check(status should equal(OK))
        }
      }
    }
  }

  def createAcc(accout: Account, asst: Assistant): HttpRequest =
    Post("/account/assistant", FormData(
      "net_id" -> asst.netId,      "user"  -> accout.username,    "pass"       -> accout.passwd,
      "email"  -> asst.email,      "rate"  -> asst.rate.toString, "job"        -> asst.job,
      "dept"   -> asst.department, "first" -> asst.firstName,     "last"       -> asst.lastName,
      "emp_id" -> asst.employeeId, "title" -> asst.title,         "title_code" -> asst.titleCode
    ))

}
