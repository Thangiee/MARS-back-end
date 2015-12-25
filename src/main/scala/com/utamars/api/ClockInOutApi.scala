package com.utamars.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.std.all._
import com.github.nscala_time.time.Imports._
import com.utamars.dataaccess.{NotFound, _}
import com.utamars.forms.UpdateRecordForm
import spray.json._

import scala.concurrent.{Await, ExecutionContext}
import scalacache.{ScalaCache, get => _}


case class ClockInOutApi(implicit cache: ScalaCache, sm: SessMgr, rts: RTS, ec: ExecutionContext) extends Api {

  override val defaultAuthzRoles: Seq[Role] = Seq(Role.Assistant)
  override val realm            : String    = "mars-app"

  override val route: Route =
    logRequestResult("Clocking In") {
      (post & path("records"/"clock-in") & formFields('uuid, 'computerid) & authnAndAuthz()) { (uuid, compId, account) =>
        scalacache.sync.get(uuid) match {
          case Some(_) => processClockInRequest(compId, account) // successful found the registered UUID
          case None    => complete(HttpResponse(Gone)) // did not find the UUID, either it was not registered or it has been expired
        }
      }
    } ~
    logRequestResult("Clock Out") {
      (post & path("records"/"clock-out") & formField('uuid, 'computerid) & authnAndAuthz()) { (uuid, compId, account) =>
        scalacache.sync.get(uuid) match {
          case Some(_) => ClockInOutRecord.clockOutAll(account.netId, compId).responseWith(OK)
          case None    => complete(HttpResponse(Gone))
        }
      }
    } ~
    (get & path("records") & authnAndAuthz()) { acc =>
      ClockInOutRecord.findBy(acc.netId).responseWith(records => records.toJson.compactPrint)
    } ~
    (get & path("records"/Segment) & authnAndAuthz(Role.Instructor)) { (netId, _) =>
      ClockInOutRecord.findBy(netId).responseWith(records => records.toJson.compactPrint)
    } ~
    ((post|put) & path("records"/IntNumber) & authnAndAuthz(Role.Instructor)) { (id, _) =>
      formFields('intime.as[Long].?, 'outtime.as[Long].?, 'incompid.?, 'outcompid.?).as(UpdateRecordForm) { form =>
        ClockInOutRecord.update(id, form).responseWith(OK)
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
