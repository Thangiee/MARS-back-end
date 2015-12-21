package com.utamars.api

import akka.http.scaladsl.model.headers.{Cookie, HttpCookie, `Set-Cookie`}
import akka.http.scaladsl.model.{DateTime, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.softwaremill.session.{SessionConfig, SessionManager, SessionUtil}
import com.utamars.dataaccess.tables.DB
import spec.ServiceSpec

class SessionSpec extends ServiceSpec {
  override val sessionConfig = SessionConfig.default(SessionUtil.randomServerSecret()).copy(
    sessionMaxAgeSeconds = Some(1),
    refreshTokenMaxAgeSeconds = 1
  )
  override implicit val sessionManager = new SessionManager[Username](sessionConfig)

  val sessionService = SessionService()
  val protectedService = new Service {
    override val route = (path("resources") & authn) { _ => complete(StatusCodes.OK) }
  }

  def loginRequest = requestWithCredentials(Post("/session/login"), adminAcc.username, adminAcc.passwd)(sessionService.route)

  val sessionCookieName = sessionConfig.sessionCookieConfig.name
  val refreshTokenCookieName = sessionConfig.refreshTokenCookieConfig.name

  def cookiesMap: Map[String, HttpCookie] = headers
    .collect { case `Set-Cookie`(cookie) => cookie.name -> cookie }.toMap

  def getSession = cookiesMap.get(sessionCookieName).map(_.value)
  def setSessionHeader() = Cookie(sessionCookieName, getSession.get)
  def isSessionExpired = cookiesMap.get(sessionCookieName).flatMap(_.expires).contains(DateTime.MinValue)

  def getRefreshToken = cookiesMap.get(refreshTokenCookieName).map(_.value)
  def setRefreshTokenHeader() = Cookie(refreshTokenCookieName, getSession.get)
  def isRefreshTokenExpired = cookiesMap.get(refreshTokenCookieName).flatMap(_.expires).contains(DateTime.MinValue)

  override def beforeAll(): Unit = {
    DB.createSchema()
    initDataBaseData()
  }

  "Session login" should {
    "include a 'Set-Cookie' in the response header" in {
      loginRequest ~> check {
        status shouldEqual StatusCodes.OK
        val sessionCookie = header[`Set-Cookie`]
        sessionCookie shouldBe defined
      }
    }
  }

  "Without credentials and a session, the system" should {
    "responses with a 403" in {
      Get("/resources") ~> Route.seal(protectedService.route) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }

  "With a session, the user" should {
    "not need to provide credentials to access protected resources" in {
      loginRequest ~> check {
        Get("/resources") ~> addHeader(setSessionHeader()) ~> addHeader(setRefreshTokenHeader()) ~>
          Route.seal(protectedService.route) ~> check {
          status shouldEqual StatusCodes.OK
        }
      }
    }
  }

  "After logging out, the session" should {
    "no longer be valid" in {
      loginRequest ~> check {
        status shouldEqual StatusCodes.OK

        Get("/session/logout") ~> addHeader(setSessionHeader()) ~> addHeader(setRefreshTokenHeader()) ~>
          sessionService.route ~> check {
          Get("/resources") ~>
            Route.seal(protectedService.route) ~> check {
            status shouldEqual StatusCodes.Forbidden
          }
        }
      }
    }
  }

  "After the session expired, it" should {
    "no longer be valid" in {
      loginRequest ~> check {
        status shouldEqual StatusCodes.OK
        Get("/resources") ~> addHeader(setSessionHeader()) ~> addHeader(setRefreshTokenHeader()) ~>
          Route.seal(protectedService.route) ~> check {
          status shouldEqual StatusCodes.OK
        }

        Thread.sleep(1100)

        Get("/resources") ~> addHeader(setSessionHeader()) ~> addHeader(setRefreshTokenHeader()) ~>
          Route.seal(protectedService.route) ~> check {
          status shouldEqual StatusCodes.Forbidden
        }
      }
    }
  }


}
