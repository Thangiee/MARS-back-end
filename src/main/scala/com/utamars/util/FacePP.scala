package com.utamars.util

import java.net.{ConnectException, SocketTimeoutException}

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import cats.data.{Xor, XorT}
import cats.std.all._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import spray.json._
import spray.json.lenses.JsonLenses._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scalaj.http.{HttpResponse => JHttpResponse, _}

trait FacePP {
  type FaceId       = String
  type Confidence   = Double
  type IsSamePerson = Boolean

  def detectionDetect(url: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, FaceId]
  def recognitionVerify(personName: String, faceId: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, (Confidence, IsSamePerson)]
  def personAddFace(personName: String, faceId: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, Unit]
  def personRemoveFace(personName: String, faceId: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, Unit]
  def personCreate(personName: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, Unit]
  def personDelete(personName: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, Unit]
  def trainVerify(personName: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, Unit]
}

// wrapper for http://www.faceplusplus.com/api-overview/
object FacePP extends AnyRef with DefaultJsonProtocol with LazyLogging {

  implicit val defaultFacePP = new FacePP {
    private val baseUrl = "https://apius.faceplusplus.com/v2"
    private val config = ConfigFactory.load()

    private def POST(route: String) = Http(baseUrl + route).timeout(10000, 10000).method("POST")
      .params("api_secret" -> config.getString("facepp.secret"), "api_key" -> config.getString("facepp.key"))

    def detectionDetect(url: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, FaceId] =
      call(POST("/detection/detect").param("url", url)).flatMap(json =>
        json.extract[String]('face / * / 'face_id).headOption match {
          case Some(id) => XorT.right[Future, HttpResponse, String](Future.successful(id))
          case None => XorT.left[Future, HttpResponse, String](Future.successful(HttpResponse(BadRequest, entity = "Cannot detect face in the photo.")))
        }
      )

    def recognitionVerify(personName: String, faceId: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, (Confidence, IsSamePerson)] =
      call(POST("/recognition/verify").params("person_name" -> personName, "face_id" -> faceId)).map(json => {
        (json.extract[Double]('confidence), json.extract[Boolean]('is_same_person))
      })

    def personAddFace(personName: String, faceId: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, Unit] =
      call(POST("/person/add_face").params("person_name" -> personName, "face_id" -> faceId)).map(_ => Unit)

    def personRemoveFace(personName: String, faceId: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, Unit] =
      call(POST("/person/remove_face").params("person_name" -> personName, "face_id" -> faceId)).map(_ => Unit)

    def personCreate(personName: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, Unit] =
      call(POST("/person/create").params("person_name" -> personName)).map(_ => Unit)

    def personDelete(personName: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, Unit] =
      call(POST("/person/delete").params("person_name" -> personName)).map(_ => Unit)

    def trainVerify(personName: String)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, Unit] =
      call(POST("/train/verify").params("person_name" -> personName)).map(_ => Unit)


    private def call(request: HttpRequest)(implicit ec: ExecutionContext): XorT[Future, HttpResponse, JsValue] = XorT(Future {
      Try(request.asString) match {
        case Success(JHttpResponse(body, 200, _)) => logger.info(s"face++: $body"); Xor.Right(body.parseJson)
        case Success(JHttpResponse(body, code, _)) => Xor.Left(faceppErrHandler(code, body))
        case Failure(ex) => Xor.Left(exceptionToError(ex))
      }
    })

    private def faceppErrHandler(code: Int, body: String): HttpResponse = code match {
      case 403 | 500 | 502 =>
        logger.error(s"Face++: $code, $body")
        HttpResponse(500, entity = InternalServerError.defaultMessage)
      case 431 =>
        HttpResponse(413, entity = "The image is too large.")
      case 441 =>
        HttpResponse(400, entity = "Face++: Unable to do recognition since there is no face associated with this assistant. Please add a face first.")
      case 453 =>
        logger.error("Face++: Try to create a person (http://www.faceplusplus.com/personcreate/) but the name already exists.")
        HttpResponse(500, entity = InternalServerError.defaultMessage)
      case _ =>
        logger.error(s"Face++: $code, $body")
        HttpResponse(code, entity = body)
    }

    private def exceptionToError(ex: Throwable): HttpResponse = ex match {
      case ex: ConnectException =>
        logger.info(s"Face++ ConnectException: ${ex.getMessage}")
        HttpResponse(503, entity = ServiceUnavailable.defaultMessage)
      case ex: SocketTimeoutException =>
        logger.info(s"Face++ SocketTimeoutException: ${ex.getMessage}")
        HttpResponse(503, entity = ServiceUnavailable.defaultMessage)
      case _ =>
        logger.error(ex.getMessage, ex)
        HttpResponse(500, entity = InternalServerError.defaultMessage)
    }
  }
}
