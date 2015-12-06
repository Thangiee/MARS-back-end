package com.utamars.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.{RefreshTokenStorage, SessionManager}
import com.utamars.dataaccess.Role

import scala.concurrent.ExecutionContext.Implicits.global

case class SessionService(implicit sm: SessionManager[Username], ts: RefreshTokenStorage[Username]) extends Service {
  override val authzRoles = Seq(Role.Admin, Role.Instructor, Role.Assistant)

  override val route = pathPrefix("session") {
    logRequestResult("session-login") {
      (post & path("login") & authnAndAuthz) { acc =>
        setSession(refreshable[String], usingCookies, acc.username) {
          complete(StatusCodes.OK)
        }
      }
    } ~
    logRequest("session-logout") {
      (get & path("logout") & invalidateSession(refreshable[String], usingCookies)) {
        complete(StatusCodes.OK)
      }
    }
  }
}
