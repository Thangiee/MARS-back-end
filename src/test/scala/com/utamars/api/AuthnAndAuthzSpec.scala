package com.utamars.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.utamars.ServiceSpec
import com.utamars.dataaccess.Role

class AuthnAndAuthzSpec extends ServiceSpec {

  val testService = new Service {
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
    val request  = requestWithCredentials(Get("/admin-and-inst-only"), Route.seal(testService.route)) _
    val request2 = requestWithCredentials(Get("/asst-only"), Route.seal(testService.route)) _

    "response with 200 on authenticated and authorized account" in {
      Seq(adminAcc, instAliceAcc).foreach{ acc =>
        request(acc.username, acc.passwd) ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[String] shouldEqual acc.username
        }
      }

      request2(asstBobAcc.username, asstBobAcc.passwd) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual asstBobAcc.username
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
      request(asstBobAcc.username, asstBobAcc.passwd) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }

      request2(adminAcc.username, adminAcc.passwd) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }
}
