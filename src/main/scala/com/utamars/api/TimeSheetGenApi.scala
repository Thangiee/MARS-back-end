package com.utamars.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.std.all._
import com.github.nscala_time.time.Imports._
import com.utamars.dataaccess._
import com.utamars.util.EMailer

import scala.concurrent.ExecutionContext

case class TimeSheetGenApi(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  private val monthYear =  parameter('month.as[Int], 'year.as[Int])

  override val route = logRequestResult("generate time-sheet") {
    (get & pathPrefix("time-sheet") & authnAndAuthz(Role.Assistant)) { acc =>
      (path("first-half-month") & monthYear) { (month, year) =>
        asstMakeAndSendTimeSheet(acc.netId, month, year, isFirst = true)
      } ~
      (path("second-half-month") & monthYear) { (month, year) =>
        asstMakeAndSendTimeSheet(acc.netId, month, year, isFirst = false)
      }
    } ~
    (get & pathPrefix("time-sheet") & authnAndAuthz(Role.Instructor)) { acc =>
      (path(Segment / "first-half-month") & monthYear) { (netId, month, year) =>
        InstMakeAndSendTimeSheet(acc, netId, month, year, isFirst = true)
      } ~
      (path(Segment / "second-half-month") & monthYear) { (netId, month, year) =>
        InstMakeAndSendTimeSheet(acc, netId, month, year, isFirst = false)
      }
    }
  }

  private def InstMakeAndSendTimeSheet(acc: Account, netId: String, month: Int, year: Int, isFirst: Boolean): Route = {
    if ((1 to 12).contains(month) && (year > 0)) {
      val result = for {
        inst      <- Instructor.findBy(acc.netId)
        asst      <- Assistant.findBy(netId)
        payPeriod =  halfMonth(month, year, isFirst)
        timeSheet <- TimeSheet.fromDateRange(payPeriod, asst)
        _         = EMailer.mailTo(inst.email, subject = timeSheet.nameWithoutExtension.replace("_", " "), timeSheet)
      } yield ()

      result.responseWith(OK)
    } else {
      complete(HttpResponse(BadRequest, entity = s"Not a valid date: month=$month year=$year"))
    }
  }

  private def asstMakeAndSendTimeSheet(netId: String, month: Int, year: Int, isFirst: Boolean): Route = {
    if ((1 to 12).contains(month) && (year > 0)) {
      val result = for {
        asst      <- Assistant.findBy(netId)
        payPeriod =  halfMonth(month, year, isFirst)
        timeSheet <- TimeSheet.fromDateRange(payPeriod, asst)
        _         = EMailer.mailTo(asst.email, subject = timeSheet.nameWithoutExtension.replace("_", " "), timeSheet)
      } yield ()

      result.responseWith(OK)
    } else {
      complete(HttpResponse(BadRequest, entity = s"Not a valid date: month=$month year=$year"))
    }
  }

  private def halfMonth(m: Int, y: Int, first: Boolean): (LocalDate, LocalDate) = {
    if (first) {  // first half of the month; day 1 to 15.
      val start = new LocalDate(y, m, 1)
      val end   = new LocalDate(y, m, 15)
      (start, end)
    } else {      // second half of the month; day 16 to last day of the month
      val start = new LocalDate(y, m, 16)
      val end   = new LocalDate(y, m, 28).dayOfMonth().withMaximumValue()
      (start, end)
    }
  }
}