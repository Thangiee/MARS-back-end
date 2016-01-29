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
    (post & path("records"/"clock-in") & formFields('computerid.?) & authnAndAuthz()) { (compId, account) =>
      processClockInRequest(compId, account)
    } ~
    (post & path("records"/"clock-out") & formField('computerid.?) & authnAndAuthz()) { (compId, account) =>
      ClockInOutRecord.clockOutAll(account.netId, compId).responseWith(OK)
    } ~
    (get & path("records") & parameter('filter.?) & authnAndAuthz()) { (dateFilter, acc) =>
      getRecords(dateFilter, acc.netId)
    } ~
    (get & path("records"/Segment) & parameter('filter.?) & authnAndAuthz(Role.Instructor)) { (netId, dateFilter, _) =>
      getRecords(dateFilter, netId)
    } ~
    ((post|put) & path("records"/IntNumber) & authnAndAuthz(Role.Instructor)) { (id, _) =>
      formFields('intime.as[Long].?, 'outtime.as[Long].?, 'incompid.?, 'outcompid.?).as(UpdateRecordForm) { form =>
        ClockInOutRecord.update(id, form).responseWith(OK)
      }
    }

  private def processClockInRequest(compId: Option[String], account: Account): Route = {
    def createClockInRecord(): HttpResponse =
      Await.result(ClockInOutRecord(None, account.netId, inComputerId = compId).create()
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

  private def getRecords(dateFilter: Option[String], netId: String): Route = {
    val today = LocalDate.now()
    dateFilter match {
      case Some("pay-period")  =>
        val (start, end) = halfMonth(today.getMonthOfYear, today.getYear, first = today.getDayOfMonth < 16)
        ClockInOutRecord.findBetween(start, end, netId).responseWith(records => Map("records" -> records).toJson.compactPrint)

      case Some("month")       =>
        val start = today.dayOfMonth().withMaximumValue()
        val end   = today.dayOfMonth().withMaximumValue()
        ClockInOutRecord.findBetween(start, end, netId).responseWith(records => Map("records" -> records).toJson.compactPrint)

      case Some("year") =>
        val start = today.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue()
        val end   = today.monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue()
        ClockInOutRecord.findBetween(start, end, netId).responseWith(records => Map("records" -> records).toJson.compactPrint)

      case Some(_) => complete(HttpResponse(BadRequest, entity = "Invalid parameter: filter"))

      case None => // no filter, get all records
        ClockInOutRecord.findBy(netId).responseWith(records => Map("records" -> records).toJson.compactPrint)
    }
  }
}
