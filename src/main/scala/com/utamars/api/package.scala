package com.utamars

import java.sql.Timestamp

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.{Xor, XorT}
import com.softwaremill.session.{RefreshTokenStorage, SessionManager}
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess._
import com.utamars.util.TimeConversion
import spray.json._

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success}

package object api extends AnyRef with TimeConversion with DefaultJsonProtocol with LazyLogging {

  type Username = String
  type ErrMsg = String

  // abbreviation
  type SessMgr = SessionManager[Username]
  type RTS = RefreshTokenStorage[Username]

  // needed to be able to convert Map[String, Any] to json using spray
  implicit val anyJsonFormat = new JsonWriter[Any] {
    def write(obj: Any): JsValue = obj match {
      case n: Int           => JsNumber(n)
      case n: Long          => JsNumber(n)
      case n: Short         => JsNumber(n)
      case s: String        => JsString(s)
      case b: Boolean if b  => JsTrue
      case b: Boolean if !b => JsFalse
      case _ => deserializationError("Unsupported type")
    }
  }

  implicit val timestampJsonFormat = new JsonFormat[Timestamp] {
    def write(ts: Timestamp): JsValue = JsNumber(ts.getTime)
    def read(json: JsValue): Timestamp = json match {
      case JsNumber(x) => new Timestamp(x.longValue)
      case x => deserializationError("Expected Timestamp as JsNumber, but got " + x)
    }
  }

  implicit val accJsonFormat = jsonFormat5(Account.apply)

  implicit val asstJsonFormat = jsonFormat10(Assistant.apply)

  implicit val instJsonFormat = jsonFormat4(Instructor.apply)

  implicit class XorFuture2Route[A](future: XorT[Future, DataAccessErr, A]) {
    def responseWith(onSucc: A => ToResponseMarshallable, onErr: DataAccessErr => ToResponseMarshallable = (err) => err.toHttpResponse): Route =
      onComplete(future.value) {
        case Success(Xor.Right(a))  => complete(onSucc(a))
        case Success(Xor.Left(err)) => complete(onErr(err))
        case Failure(ex) => logger.error(ex.getMessage, ex); complete(HttpResponse(StatusCodes.InternalServerError))
      }

    def responseWith(code: StatusCode): Route = onComplete(future.value) {
      case Success(Xor.Right(a))  => complete(HttpResponse(code))
      case Success(Xor.Left(err)) => complete(err.toHttpResponse)
      case Failure(ex) => logger.error(ex.getMessage, ex); complete(HttpResponse(StatusCodes.InternalServerError))
    }
  }

  implicit class DataAccessErr2HttpResponse(err: DataAccessErr) {
    def toHttpResponse: HttpResponse = err match {
      case NotFound             => HttpResponse(StatusCodes.NotFound)
      case SqlDuplicateKey(msg) => HttpResponse(StatusCodes.Conflict, entity = msg)
      case SqlErr(code, msg) =>
        logger.error(s"SQL error code: $code | $msg")
        HttpResponse(StatusCodes.InternalServerError)
      case InternalErr(error)  =>
        logger.error(error.getMessage)
        error.printStackTrace()
        HttpResponse(StatusCodes.InternalServerError)
    }
  }

}
