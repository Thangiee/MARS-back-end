package com.utamars

import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess.{InternalErr, SqlErr, NotFound, DataAccessErr}
import spray.json._

import scala.concurrent.duration.{TimeUnit, DurationConversions, Duration, FiniteDuration}

package object api extends AnyRef with LazyLogging {

  type Username = String

  // needed to be able to convert Map[String, Any] to json using spray
  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case n: Int           => JsNumber(n)
      case n: Long          => JsNumber(n)
      case n: Short         => JsNumber(n)
      case s: String        => JsString(s)
      case b: Boolean if b  => JsTrue
      case b: Boolean if !b => JsFalse
    }
    def read(value: JsValue) = {}
  }

  implicit class DataAccessErr2HttpResponse(err: DataAccessErr) {
    def toHttpResponse: HttpResponse = err match {
      case NotFound            => HttpResponse(StatusCodes.NotFound)
      case SqlErr(msg, detail) =>
        logger.debug(detail)
        HttpResponse(StatusCodes.Conflict, entity = msg)
      case InternalErr(error)    =>
        logger.error(error.getMessage)
        error.printStackTrace()
        HttpResponse(StatusCodes.InternalServerError)
    }
  }

  // Taken from scala.concurrent.duration package object to make using
  // duration conversions easily without needing to import scala.concurrent.duration._
  implicit final class DurationInt(private val n: Int) extends AnyVal with DurationConversions {
    override protected def durationIn(unit: TimeUnit): FiniteDuration = Duration(n.toLong, unit)
  }

  implicit final class DurationLong(private val n: Long) extends AnyVal with DurationConversions {
    override protected def durationIn(unit: TimeUnit): FiniteDuration = Duration(n, unit)
  }

  implicit final class DurationDouble(private val d: Double) extends AnyVal with DurationConversions {
    override protected def durationIn(unit: TimeUnit): FiniteDuration =
      Duration(d, unit) match {
        case f: FiniteDuration => f
        case _ => throw new IllegalArgumentException("Duration DSL not applicable to " + d)
      }
  }
  // ==================================================================
}
