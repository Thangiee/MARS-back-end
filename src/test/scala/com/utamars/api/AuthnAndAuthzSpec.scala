package com.utamars.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.utamars.dataaccess.Role
import spec.ServiceSpec

class AuthnAndAuthzSpec extends ServiceSpec {

  val testService = new Service {
    override val authzRoles: Seq[Role] = Seq(Role.Admin, Role.Instructor)
    override val route: Route = (path("test") & authnAndAuthz) { account =>
      complete(account.username)
    }
  }

  override def beforeAll(): Unit = {
    initDataBase()
  }

  "Services with authentication and Authorization" should {
    val request = requestWithCredentials(Get("/test"), Route.seal(testService.route)) _

    "response with 200 on authenticated and authorized account" in {
      Seq(adminAcc, instructorAliceAcc).foreach{ acc =>
        request(acc.username, acc.passwd) ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[String] shouldEqual acc.username
        }
      }
    }

    "response with 401 on invalid username and/or password" in {
      val userAndPass = Seq(
        ("bad_username", "bad_passwd"),
        (adminAcc.username, "bad_passwd"),
        ("bad_username", adminAcc.passwd))

      userAndPass.foreach { case (user, passwd) =>
        request(user, passwd) ~> check {
          status shouldEqual StatusCodes.Unauthorized
        }
      }
    }

    "response with 403 on authenticated account but not authorized" in {
      request(assistantBobAcc.username, assistantBobAcc.passwd) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }
}
