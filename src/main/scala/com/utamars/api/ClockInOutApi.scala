package com.utamars.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.std.all._
import com.github.nscala_time.time.Imports._
import com.softwaremill.session.{SessionManager, _}
import com.utamars.dataaccess.{NotFound, _}

import scala.concurrent.{Await, ExecutionContext}
import scalacache.{ScalaCache, get => _}


case class ClockInOutApi(implicit cache: ScalaCache, sm: SessionManager[Username],
  ts: RefreshTokenStorage[Username], ec: ExecutionContext) extends Api {

  override val defaultAuthzRoles: Seq[Role] = Seq(Role.Assistant)
  override val realm            : String    = "mars-app"

  override val route: Route = post {
    logRequestResult("Clocking In") {
      (post & path("clock-in") & formFields('uuid, 'computerid) & authnAndAuthz()) { (uuid, compId, account) =>
        complete {
          scalacache.get(uuid).map {
            case Some(_) => // successful found the registered UUID
              processClockInRequest(compId, account)
            case None    => // did not find the UUID, either it was not registered or it has been expired
              HttpResponse(Gone)
          }
        }
      }
    } ~
    logRequestResult("Clock Out") {
      (path("clock-out") & formField('uuid, 'computerid) & authnAndAuthz()) { (uuid, compId, account) =>
        complete {
          scalacache.get(uuid).map {
            case Some(_) =>
              processClockOutRequest(compId, account)
            case None    =>
              HttpResponse(Gone)
          }
        }
      }
    }
  }

  private def processClockInRequest(compId: String, account: Account) = {
    def createClockInRecord() =
      Await.result(ClockInOutRecord(None, account.netId, inComputerId = Some(compId)).create()
        .fold(err => err.toHttpResponse, _ => HttpResponse(OK)), 1.minute)

    val result = ClockInOutRecord.findMostRecent(account.netId).map { record =>
      // check for clock out of previous record before creating a new record
      if   (record.outTime.isDefined) createClockInRecord()
      else HttpResponse(Conflict, entity = "Please clock out first then try again.")
    } leftMap {
      // no previous record
      case NotFound => createClockInRecord()
      case otherErr => otherErr.toHttpResponse
    }

    Await.result(result.merge, 1.minute)
  }

  private def processClockOutRequest(compId: String, account: Account) = {
    val result = ClockInOutRecord.clockOutAll(account.netId, compId)
      .fold(err => err.toHttpResponse, _ => HttpResponse(OK))

    Await.result(result, 1.minute)
  }
}
