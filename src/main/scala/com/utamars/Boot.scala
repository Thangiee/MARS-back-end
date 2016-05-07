package com.utamars

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server.directives.LogEntry
import akka.http.scaladsl.server.{AuthenticationFailedRejection, RejectionHandler}
import akka.stream.ActorMaterializer
import com.github.nscala_time.time.Imports._
import com.softwaremill.session._
import com.typesafe.scalalogging.LazyLogging
import com.utamars.CustomRejection.NotApprove
import com.utamars.api._
import com.utamars.dataaccess.DB
import com.utamars.tasks.{ClockOutAndNotify, GenAndEmailAllAsstTS}
import com.utamars.util.TimeImplicits
import com.utamars.ws.ClockInTracker

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scalacache.ScalaCache
import scalacache.guava._

object Boot extends App with CorsSupport with TimeImplicits with LazyLogging {
  val sessionConfig = SessionConfig.default(SessionUtil.randomServerSecret()).copy(
    sessionMaxAgeSeconds = Some(24.hours.toSeconds),
    sessionEncryptData = true,
    refreshTokenMaxAgeSeconds = 14.days.seconds
  )

  implicit val system       = ActorSystem("MARS", com.utamars.util.Config.config)
  implicit val dispatcher   = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val tracker      = new ClockInTracker()

  // AIPs' dependencies that will be injected implicitly
  implicit val scalaCache   = ScalaCache(GuavaCache())
  implicit val sessionManager = new SessionManager[Username](sessionConfig)
  implicit val sessionStorage = new RefreshTokenStorage[Username] { // todo: better to store in DB?
    override def lookup(selector: String): Future[Option[RefreshTokenLookupResult[Username]]] =
      scalacache.get[RefreshTokenData[Username]](selector).map {
        case Some(token) => Some(RefreshTokenLookupResult[Username](token.tokenHash, token.expires, () => token.forSession))
        case None => None
      }
    override def schedule[S](after: Duration)(op: => Future[S]): Unit = Future { Thread.sleep(after.toMillis); op }
    override def store(data: RefreshTokenData[Username]): Future[Unit] = scalacache.put(data.selector)(data)
    override def remove(selector: String): Future[Unit] = scalacache.remove(selector)
  }

  if (util.Config.createSchema) DB.createSchema()

  val APIs =
    AccountApi() ::
    AssistantApi() ::
    InstructorApi() ::
    SessionApi() ::
    RegisterUUIDApi() ::
    ClockInOutApi().onClockInOrOut(tracker.refresh())   ::
    TimeSheetGenApi() ::
    FacialRecognitionApi() ::
    AssetsApi() ::
    Nil

  val requestLog = (req: HttpRequest, info: String) =>
    s"\nRequest $info:" +
      s"\n\t${req.method} ${req.uri}" +
      s"\n\tHeaders: ${req.headers.mkString(", ")}" +
      s"\n\t${req.entity.toString.split("\n").take(3).mkString("\n  ")}"

  def customLogging(req: HttpRequest): Any => Option[LogEntry] = {
    case Complete(response) => Some(LogEntry(requestLog(req, s"[${response.status}]"), Logging.InfoLevel))
    case Rejected(_)        => Some(LogEntry(requestLog(req, "[Rejected]"), Logging.InfoLevel))
    case _                  => None // other kind of responses
  }

  implicit def myRejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case AuthenticationFailedRejection(_, _) => complete(HttpResponse(Unauthorized, entity = "Invalid username or password."))
      case NotApprove() => complete(HttpResponse(Forbidden, entity = "This account has not been approve by the administrator."))
    }
    .result()

  val routes = logRequestResult(customLogging _) {
    pathPrefix("api")(cors(APIs.map(_.route).reduce(_ ~ _))) ~ WebSocket().route
  }

  // check every day at ~11:59pm for the end of the pay period date
  // to generate and email timesheet for all assistants.
  val timeTil7Pm = (new LocalTime(23, 59) - LocalTime.now().getMillisOfDay.millis).getMillisOfDay
  system.scheduler.schedule(timeTil7Pm.millis, 24.hours) {
    val today = DateTime.now()
    val (_, payPeriodEnd) = today.toLocalDate.halfMonth
    if (today.getDayOfMonth == payPeriodEnd.getDayOfMonth) GenAndEmailAllAsstTS().run()
  }

  // run every day at ~5:00am to clock out any assistants still
  // clocked in and notify them via email.
  val timeTil5AmNextDay = ((DateTime.now().withHour(5).withMinute(0) + 1.day) - DateTime.now().getMillis).getMillis.toInt
  system.scheduler.schedule(timeTil5AmNextDay.millis, 24.hours)(ClockOutAndNotify().onClockOut(tracker.refresh()).run())

  logger.info(s"Listening on ${util.Config.privateAddr}:${util.Config.port}")
  Http().bindAndHandle(routes, util.Config.privateAddr, util.Config.port)
}

