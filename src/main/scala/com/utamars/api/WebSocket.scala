package com.utamars.api

import java.util.UUID

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.utamars.dataaccess.Role
import com.utamars.ws.ClockInTracker

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scalacache.ScalaCache

case class WebSocket(implicit cache: ScalaCache ,sm: SessMgr, rts: RTS, ec: ExecutionContext, tracker: ClockInTracker) extends Api {

  val route: Route =
    (get & path("ws"/"current-clock-in-assts-tracker"/JavaUUID)) { token =>
      scalacache.sync.get[String](token.toString) match {
        case Some("web-socket") => scalacache.remove(token.toString); handleWebSocketMessages(tracker.webSocketFlow())
        case _                  => complete(HttpResponse(401, entity = "Invalid web socket token"))
      }
    } ~
    (get & path("api"/"web-socket-token") & authnAndAuthz(Role.Admin, Role.Instructor) ) { _ =>
      val token = UUID.randomUUID().toString
      scalacache.sync.cachingWithTTL(token)(1.minute)("web-socket")
      complete(Map("token" -> token).jsonCompat)
    }

}
