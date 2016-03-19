package com.utamars.util

import java.sql.Timestamp

import com.utamars.api.DAOs.{AccountDAO, AssistantDAO, InstructorDAO}
import com.utamars.dataaccess.{Account, ClockInAsst, ClockInOutRecord}
import spray.json.{JsonFormat, _}

trait JsonImplicits extends AnyRef with DefaultJsonProtocol with NullOptions {

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

  implicit val accFmt     = jsonFormat6(Account.apply)
  implicit val jsonFmt    = jsonFormat5(ClockInAsst.apply)
  implicit val recordFmt  = jsonFormat6(ClockInOutRecord.apply)
  implicit val accDAOFmt  = jsonFormat5(AccountDAO.apply)
  implicit val asstDAOFmt = jsonFormat15(AssistantDAO.apply)
  implicit val instDAOFmt = jsonFormat8(InstructorDAO.apply)

  implicit class JsonOps[T](data: T) {
    def jsonCompat(implicit writer : JsonWriter[T]): String = data.toJson.compactPrint
  }

}

object JsonImplicits extends JsonImplicits
