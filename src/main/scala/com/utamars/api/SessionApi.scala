package com.utamars.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session.{RefreshTokenStorage, SessionManager}
import com.utamars.dataaccess.Role

import scala.concurrent.ExecutionContext.Implicits.global

case class SessionApi(implicit sm: SessMgr, rts: RTS) extends Api {
  override val defaultAuthzRoles = Seq(Role.Admin, Role.Instructor, Role.Assistant)

  override val route = pathPrefix("session") {
    ((post|put) & path("login") & authnAndAuthz()) { acc =>
      setSession(refreshable[String], usingCookies, acc.username) {
        complete(StatusCodes.OK)
      }
    } ~
    ((post|put) & path("logout") & invalidateSession(refreshable[String], usingCookies)) {
      complete(StatusCodes.OK)
    }
  }

}
