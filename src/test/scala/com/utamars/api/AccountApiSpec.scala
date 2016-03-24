package com.utamars.api

import akka.http.scaladsl.model.{FormData, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import cats.data.XorT
import cats.std.all._
import com.utamars.{ExeCtx, ApiSpec}
import com.utamars.api.DAOs.AccountDAO
import com.utamars.dataaccess.{Account, Role}
import com.utamars.util.FacePP

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scalacache.ScalaCache
import scalacache.guava.GuavaCache

class AccountApiSpec extends ApiSpec {

  implicit val mockFacePP = new MockFacePP {
    override def personDelete(personName: String)(implicit ec: ExeCtx) = XorT.right[Future, HttpResponse, Unit](Future.successful(()))
    override def personCreate(personName: String)(implicit ec: ExeCtx) = XorT.right[Future, HttpResponse, Unit](Future.successful(()))
  }

  implicit val scalaCache = ScalaCache(GuavaCache())

  val api = AccountApi()

  override def beforeAll(): Unit = {
    initDataBase()
    initDataBaseData()
  }

  "Any user" must {
    "be able to get their account info" in {
      adminRequest(_ => Get("/account")) ~> check {
        responseTo[AccountDAO].netId should equal (adminAcc.netId)
      }
    }
  }

  "Admin user" must {
    "be able to get all account info" in {
      adminRequest(_ => Get("/account/all")) ~> check {
        responseToSeq[AccountDAO]('accounts) should not be empty
      }
    }

    "be able to delete account" in {
      val temp = Account("99998", "temp", "password", Role.Instructor, approve = true)
      Await.result(temp.create().value, 10.seconds)

      adminRequest(_ => Get(s"/account/${temp.netId}")) ~> check(responseTo[AccountDAO].netId should equal(temp.netId))
      adminRequest(_ => Delete(s"/account/${temp.netId}")) ~> check(status should equal(OK))
      adminRequest(_ => Get(s"/account/${temp.netId}")) ~> check(status should equal(NotFound))
    }

    "be able to enable/disable account" in {
      adminRequest(_ => Post(s"/account/change-approve/${asstBobAcc.netId}", FormData("approve" -> "false"))) ~> check {
        asstRequest(_ => Get("/account")) ~> check {
          status should equal(Forbidden)
        }

        // reset the approval
        adminRequest(_ => Post(s"/account/change-approve/${asstBobAcc.netId}", FormData("approve" -> "true"))) ~> check {
          asstRequest(_ => Get("/account")) ~> check {
            status should equal(OK)
          }
        }
      }
    }

    "be able to change any account password" in {
      adminRequest(_ => Post(s"/account/change-password/${asstBobAcc.netId}", FormData("new_password" -> "123"))) ~> check {
        asstRequest(_ => Get("/account")) ~> check {
          status should equal(Unauthorized)
        }

        // reset the password
        adminRequest(_ => Post(s"/account/change-password/${asstBobAcc.netId}", FormData("new_password" -> asstBobAcc.passwd))) ~> check {
          asstRequest(_ => Get("/account")) ~> check {
            status should equal(OK)
          }
        }
      }
    }
  }
}
