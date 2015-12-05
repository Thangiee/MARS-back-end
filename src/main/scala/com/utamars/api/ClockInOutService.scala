package com.utamars.api

import java.util.Date

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.Xor
import com.utamars.dataaccess._

import scala.concurrent.ExecutionContext.Implicits.global
import scalacache.{ScalaCache, get => _}


case class ClockInOutService(implicit cache: ScalaCache) extends Service {

  override val authzRoles: Seq[Role] = Seq(Role.Assistant)
  override val realm     : String    = "mars-app"

  private def processClockInOutRequest(compId: String, account: Account, clockingIn: Boolean) = transaction {
    (for {
      asst <- Assistant.findByUsername(account.username)
      _   <- ClockInOutRecord(asst.employeeId, compId, new Date(), clockingIn).insert
      _   <- asst.copy(currentlyClockedIn = clockingIn).save
    } yield asst) match {
      case Xor.Right(_)  => HttpResponse(StatusCodes.OK)
      case Xor.Left(err) => err.toHttpResponse
    }
  }

  override val route: Route = post {
    logRequestResult("Clocking In") {
      (post & path("clock-in") & formFields('uuid, 'computerid) & authnAndAuthz) { (uuid, compId, account) =>
        complete {
          scalacache.get(uuid).map {
            case Some(_) => // successful found the registered UUID
              processClockInOutRequest(compId, account, clockingIn = true)
            case None    => // did not find the UUID, either it was not registered or it has been expired
              HttpResponse(StatusCodes.Gone)
          }
        }
      }
    } ~
      logRequestResult("Clock Out") {
        (path("clock-out") & formField('uuid, 'computerid) & authnAndAuthz) { (uuid, compId, account) =>
          complete {
            scalacache.get(uuid).map {
              case Some(_) =>
                processClockInOutRequest(compId, account, clockingIn = false)
              case None    =>
                HttpResponse(StatusCodes.Gone)
            }
          }
        }
      }
  }
}
