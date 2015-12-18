package com.utamars.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.softwaremill.session._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess._
import com.github.nscala_time.time.Imports._

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

  val interface = config.getString("http.interface")
  val port      = config.getInt("http.port")

  val services =
    SessionService() ::
    RegisterUUIDService() ::
    ClockInOutService()   ::
    FacialRecognitionService() ::
    TimeSheetGenService() ::
    Nil

  val routes   = pathPrefix("api") { services.map(_.route).reduce(_ ~ _) }
  Http().bindAndHandle(routes, interface, port)

  if (config.getBoolean("db.create")) transaction {
    MySchema.create
    MySchema.printDdl
  }
}

