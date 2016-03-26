package com.utamars.api

import akka.http.scaladsl.model.headers.{Cookie, HttpCookie, `Set-Cookie`}
import akka.http.scaladsl.model.{DateTime, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.softwaremill.session.{SessionConfig, SessionManager, SessionUtil}
import com.utamars.ApiSpec

class SessionApiSpec extends ApiSpec {
  override val sessionConfig = SessionConfig.default(SessionUtil.randomServerSecret()).copy(
    sessionMaxAgeSeconds = Some(1),
    refreshTokenMaxAgeSeconds = 1
  )
  override implicit val sessionManager = new SessionManager[Username](sessionConfig)

  val api = SessionApi()
  val protectedService = new Api {
    override val route = (path("resources") & authn) { _ => complete(StatusCodes.OK) }
  }

  def loginRequest = adminRequest(Post("/session/login"))

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
    initDataBase()
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
    "responses with a 401" in {
      Get("/resources") ~> Route.seal(protectedService.route) ~> check {
        status shouldEqual StatusCodes.Unauthorized
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

        Post("/session/logout") ~> addHeader(setSessionHeader()) ~> addHeader(setRefreshTokenHeader()) ~>
          api.route ~> check {
          Get("/resources") ~>
            Route.seal(protectedService.route) ~> check {
            status shouldEqual StatusCodes.Unauthorized
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
          status shouldEqual StatusCodes.Unauthorized
        }
      }
    }
  }


}
