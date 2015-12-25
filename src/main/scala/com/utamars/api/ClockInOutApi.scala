package com.utamars.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.std.all._
import com.github.nscala_time.time.Imports._
import com.utamars.dataaccess.{NotFound, _}

import scala.concurrent.{Await, ExecutionContext}
import scalacache.{ScalaCache, get => _}


case class ClockInOutApi(implicit cache: ScalaCache, sm: SessMgr, rts: RTS, ec: ExecutionContext) extends Api {

  override val defaultAuthzRoles: Seq[Role] = Seq(Role.Assistant)
  override val realm            : String    = "mars-app"

  override val route: Route = post {
    logRequestResult("Clocking In") {
      (post & path("clock-in") & formFields('uuid, 'computerid) & authnAndAuthz()) { (uuid, compId, account) =>
        if (scalacache.sync.get(uuid).isDefined) {
          processClockInRequest(compId, account) // successful found the registered UUID
        } else {
          complete(HttpResponse(Gone)) // did not find the UUID, either it was not registered or it has been expired
        }
      }
    } ~
    logRequestResult("Clock Out") {
      (path("clock-out") & formField('uuid, 'computerid) & authnAndAuthz()) { (uuid, compId, account) =>
        if (scalacache.sync.get(uuid).isDefined) {
          ClockInOutRecord.clockOutAll(account.netId, compId).responseWith(OK)
        } else {
          complete(HttpResponse(Gone))
        }
      }
    }
  }

  private def processClockInRequest(compId: String, account: Account): Route = {
    def createClockInRecord(): HttpResponse =
      Await.result(ClockInOutRecord(None, account.netId, inComputerId = Some(compId)).create()
        .fold(err => err.toHttpResponse, succ => HttpResponse(OK)), 1.minute)

    ClockInOutRecord.findMostRecent(account.netId).responseWith(
      record => {
        // check for clock out of previous record before creating a new record
        if (record.outTime.isDefined) createClockInRecord()
        else HttpResponse(Conflict, entity = "Please clock out first then try again.")
      },
      onErr => onErr match {
        // no previous record
        case NotFound => createClockInRecord()
        case otherErr => otherErr.toHttpResponse
      }
    )
  }
}
