package com.utamars

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess._
import com.utamars.util.TimeConversion
import spray.json._

import scala.language.implicitConversions

package object api extends AnyRef with TimeConversion with LazyLogging {

  type Username = String
  type ErrMsg = String

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
        logger.debug(msg, detail)
        HttpResponse(StatusCodes.Conflict, entity = msg)
      case InternalErr(error)  =>
        logger.error(error.getMessage)
        error.printStackTrace()
        HttpResponse(StatusCodes.InternalServerError)
    }
  }

}
