package com.utamars.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.std.all._
import com.github.nscala_time.time.Imports._
import com.utamars.dataaccess.{NotFound, _}
import com.utamars.forms.UpdateRecordForm

import scala.concurrent.{Await, ExecutionContext, Future}
import scalacache.{ScalaCache, get => _}


case class ClockInOutApi(implicit cache: ScalaCache, sm: SessMgr, rts: RTS, ec: ExecutionContext) extends Api {

  override val defaultAuthzRoles: Seq[Role] = Seq(Role.Assistant)
  override val realm            : String    = "mars-app"

  override val route: Route =
    (post & path("records"/"clock-in") & formFields('computer_id.?) & authnAndAuthz()) { (compId, account) =>    // clock in
      complete(processClockInRequest(compId, account))
    } ~
    (post & path("records"/"clock-out") & formField('computer_id.?) & authnAndAuthz()) { (compId, account) =>    // clock out
      complete(ClockInOutRecord.clockOutAll(account.netId, compId).reply(_ => OK))
    } ~
    (get & path("records") & parameter('filter.?) & authnAndAuthz()) { (dateFilter, acc) =>                     // get current assistant records
      complete(getRecords(dateFilter, acc.netId))
    } ~
    (get & path("records"/"all") & authnAndAuthz(Role.Admin, Role.Instructor)) { _ =>                           // get all records
      complete(ClockInOutRecord.all().reply(records => Map("records" -> records).jsonCompat))
    } ~
    (get & path("records"/Segment) & parameter('filter.?) & authnAndAuthz(Role.Admin, Role.Instructor)) { (netId, dateFilter, _) =>  // get assistant records by net id
      complete(getRecords(dateFilter, netId))
    } ~
    ((post|put) & path("records"/IntNumber) & authnAndAuthz(Role.Admin, Role.Instructor)) { (id, _) =>          // update a record by its id
      formFields('in_time.as[Long].?, 'out_time.as[Long].?, 'in_comp_id.?, 'out_comp_id.?).as(UpdateRecordForm) { form =>
        complete(ClockInOutRecord.update(id, form).reply(_ => OK))
      }
    }

  private def processClockInRequest(compId: Option[String], account: Account): Future[Response] = {
    def createClockInRecord(): HttpResponse =
      Await.result(ClockInOutRecord(None, account.netId, inComputerId = compId).create()
        .fold(err => err2HttpResp(err), succ => HttpResponse(OK)), 1.minute)

    ClockInOutRecord.findMostRecent(account.netId).reply(
      record => {
        // check for clock out of previous record before creating a new record
        if (record.outTime.isDefined) createClockInRecord()
        else HttpResponse(Conflict, entity = "Please clock out first then try again.")
      },
      onErr => onErr match {
        case NotFound => createClockInRecord()  // no previous record
        case otherErr => err2HttpResp(otherErr)
      }
    )
  }

  private def getRecords(dateFilter: Option[String], netId: String): Future[Response] = {
    val today = LocalDate.now()
    dateFilter match {
      case Some("pay-period")  =>
        val (start, end) = today.halfMonth
        ClockInOutRecord.findBetween(start, end, netId).reply(records => Map("records" -> records).jsonCompat)

      case Some("month")       =>
        val start = today.dayOfMonth().withMinimumValue()
        val end   = today.dayOfMonth().withMaximumValue()
        ClockInOutRecord.findBetween(start, end, netId).reply(records => Map("records" -> records).jsonCompat)

      case Some("year") =>
        val start = today.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue()
        val end   = today.monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue()
        ClockInOutRecord.findBetween(start, end, netId).reply(records => Map("records" -> records).jsonCompat)

      case Some(_) => Future.successful((BadRequest, "Invalid parameter: filter"))

      case None => // no filter, get all records
        ClockInOutRecord.findByNetId(netId).reply(records => Map("records" -> records).jsonCompat)
    }
  }
}
