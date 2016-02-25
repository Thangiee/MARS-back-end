package com.utamars

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.directives.LogEntry
import akka.http.scaladsl.server.{AuthenticationFailedRejection, RejectionHandler}
import akka.stream.ActorMaterializer
import com.github.nscala_time.time.Imports._
import com.softwaremill.session._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.utamars.CustomRejection.NotApprove
import com.utamars.api._
import com.utamars.dataaccess.DB
import com.utamars.tasks.{ClockOutAndNotify, GenAndEmailAllAsstTS}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scalacache._
import scalacache.guava._

object Boot extends App with CorsSupport with LazyLogging {
  val config = ConfigFactory.load()
  val sessionConfig = SessionConfig.default(SessionUtil.randomServerSecret()).copy(
    sessionMaxAgeSeconds = Some(24.hours.toSeconds),
    sessionEncryptData = true,
    refreshTokenMaxAgeSeconds = 14.days.seconds
  )

  implicit val system       = ActorSystem("MARS", config)
  implicit val dispatcher   = system.dispatcher
  implicit val materializer = ActorMaterializer()

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

  if (config.getBoolean("db.create")) DB.createSchema()

  val APIs =
    AccountApi() ::
    SessionApi() ::
    RegisterUUIDApi() ::
    ClockInOutApi()   ::
    TimeSheetGenApi() ::
    FacialRecognitionApi() ::
    AssetsApi() ::
    Nil

  def customLogging(req: HttpRequest): Any => Option[LogEntry] = {
    case Complete(res: HttpResponse) =>
      Some(LogEntry(
        s"""|Request:
            |  ${req.method} ${req.uri}
            |  Headers: ${req.headers.mkString(", ")}
            |  ${req.entity.toString.split("\n").take(3).mkString("\n  ")}
            |  Response: $res
        """.stripMargin, Logging.InfoLevel))
    case _ => None // other kind of responses
  }

  implicit def myRejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case AuthenticationFailedRejection(_, _) => complete(HttpResponse(Unauthorized, entity = "Invalid username or password."))
      case NotApprove() => complete(HttpResponse(Forbidden, entity = "This account has not been approve by the administrator."))
    }
    .result()

  val routes = logRequestResult(customLogging _) {
    pathPrefix("api") { cors(APIs.map(_.route).reduce(_ ~ _)) }
  }

  // check every day at ~7:00pm for the end of the pay period date
  // to generate and email timesheet for all assistants.
  val timeTil7Pm = (new LocalTime(19, 0) - LocalTime.now().getMillisOfDay.millis).getMillisOfDay
  system.scheduler.schedule(timeTil7Pm.millis, 24.hours) {
    val today = DateTime.now()
    val (_, payPeriodEnd) = today.toLocalDate.halfMonth
    if (today.getDayOfMonth == payPeriodEnd.getDayOfMonth) GenAndEmailAllAsstTS().run()
  }

  // run every day at ~5:00am to clock out any assistants still
  // clocked in and notify them via email.
  val timeTil5AmNextDay = ((DateTime.now().withHour(5).withMinute(0) + 1.day) - DateTime.now().getMillis).getMillis.toInt
  system.scheduler.schedule(timeTil5AmNextDay.millis, 24.hours)(ClockOutAndNotify().run())

  val interface = config.getString("http.addr.private")
  val port      = config.getInt("http.port")
  Http().bindAndHandle(routes, interface, port)
}

