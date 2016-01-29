package com.utamars

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.directives.LogEntry
import akka.stream.ActorMaterializer
import com.github.nscala_time.time.Imports._
import com.softwaremill.session._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.utamars.api._
import com.utamars.dataaccess.DB

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scalacache._
import scalacache.guava._

object Boot extends App with LazyLogging {
  val config = ConfigFactory.load()
  val sessionConfig = SessionConfig.default(SessionUtil.randomServerSecret()).copy(
    sessionMaxAgeSeconds = Some(24.hours.toSeconds),
    sessionEncryptData = true,
    refreshTokenMaxAgeSeconds = 14.days.seconds
  )

  implicit val system       = ActorSystem("MARS", config)
  implicit val dispatcher   = system.dispatcher
  implicit val materializer = ActorMaterializer()

  // services' dependencies that will be injected implicitly
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

  val services =
    AccountApi() ::
    SessionApi() ::
    RegisterUUIDApi() ::
    ClockInOutApi()   ::
    TimeSheetGenApi() ::
    FacialRecognitionApi() ::
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

  val routes = logRequestResult(customLogging _) {
    pathPrefix("api") { services.map(_.route).reduce(_ ~ _) }
  }

  val interface = config.getString("http.addr.private")
  val port      = config.getInt("http.port")
  Http().bindAndHandle(routes, interface, port)
}

