package com.utamars.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.utamars.{Boot, ApiSpec}
import com.utamars.dataaccess.{Account, Role}

class AuthnAndAuthzSpec extends ApiSpec {

  val api = new Api {
    override val defaultAuthzRoles: Seq[Role] = Seq(Role.Admin, Role.Instructor)
    override val route: Route =
      (path("admin-and-inst-only") & authnAndAuthz()) { account => // use default authzRoles
        complete(account.username)
      } ~
      (path("asst-only") & authnAndAuthz(Role.Assistant)) { account => // override default authzRoles
        complete(account.username)
      }
  }

  override def beforeAll(): Unit = {
    initDataBase()
    initDataBaseData()
  }

  "Services with authentication and Authorization" should {
    def adminInstOnly  = requestWithCredentials(_: Account)(_ => Get("/admin-and-inst-only"))
    def asstOnly       = requestWithCredentials(_: Account)(_ => Get("/asst-only"))

    "response with 200 on authenticated, authorized, and approved account" in {
      Seq(adminAcc, instAliceAcc).foreach{ acc =>
        adminInstOnly(acc) ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[String] shouldEqual acc.username
        }
      }

      asstOnly(asstBobAcc) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual asstBobAcc.username
      }
    }

    "response with 401 on invalid username and/or password" in {
      val accs = Seq(
        adminAcc.copy(passwd = "bad_passwd"),
        adminAcc.copy(username = "bad_username")
      )

      accs.foreach { acc =>
        adminInstOnly(acc) ~> check {
          status shouldEqual StatusCodes.Unauthorized
        }
      }
    }

    "response with 403 on authenticated account but not authorized" in {
      adminInstOnly(asstBobAcc) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }

      asstOnly(adminAcc) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "response with 403 on authenticated and authorized account but not approved" in {
      asstOnly(asstEveAcc) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }
}
