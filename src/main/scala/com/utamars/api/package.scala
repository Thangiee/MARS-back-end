package com.utamars

import java.sql.Timestamp

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.{Xor, XorT}
import com.facepp.error.FaceppParseException
import com.facepp.http.HttpRequests
import com.softwaremill.session.{RefreshTokenStorage, SessionManager}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess.NotFound
import com.utamars.dataaccess._
import com.utamars.util.TimeConversion
import org.joda.time.LocalDate
import org.json.JSONException
import spray.json._

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success}

package object api extends AnyRef with TimeConversion with DefaultJsonProtocol with NullOptions with LazyLogging {

  private[api] val config = ConfigFactory.load()
  private[api] val facePlusPlus = new HttpRequests(config.getString("facepp.key"), config.getString("facepp.secret"), false, false)

  type Username = String
  type ErrMsg = String

  // abbreviation
  type SessMgr = SessionManager[Username]
  type RTS = RefreshTokenStorage[Username]

  def halfMonth(m: Int, y: Int, first: Boolean): (LocalDate, LocalDate) = {
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

  implicit val accJsonFormat    = jsonFormat5(Account.apply)
  implicit val asstJsonFormat   = jsonFormat11(Assistant.apply)
  implicit val instJsonFormat   = jsonFormat4(Instructor.apply)
  implicit val recordJsonFormat = jsonFormat6(ClockInOutRecord.apply)

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

  implicit class FaceppParseExceptionImplicit(ex: FaceppParseException) {
    def code: Int   = """(?<=responseCode=).+""".r.findFirstIn(ex.toString).getOrElse("500").toInt
    def msg: String = """(?<=message=).+(?=,)""".r.findFirstIn(ex.toString).getOrElse("Parsed empty message")
  }

  def faceppErrHandler(err: Throwable): HttpResponse = err match {
    case ex: FaceppParseException =>
      val code = ex.code
      val msg  = ex.msg
      code match {
        case 403|500|502 =>
          logger.error(s"Face++: $code, $msg", ex)
          HttpResponse(InternalServerError)
        case 431 =>
          HttpResponse(RequestEntityTooLarge, entity = "The image is too large.")
        case 441 =>
          HttpResponse(BadRequest, entity = "Face++: Unable to do recognition since there is no face associated with this assistant. Please add a face first.")
        case 453 =>
          logger.error("Face++: Try to create a person (http://www.faceplusplus.com/personcreate/) but the name already exists.")
          HttpResponse(InternalServerError)
        case _ =>
          HttpResponse(StatusCodes.custom(code, reason = msg))
      }
    case ex: JSONException =>
      HttpResponse(BadRequest, entity = "Could not detect face in the given image.")
    case ex =>
      logger.error(ex.getMessage, ex)
      HttpResponse(InternalServerError)
  }

}
