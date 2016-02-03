package com.utamars.api

import java.io.File

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import cats.std.all._
import com.utamars.dataaccess.{Account, Assistant, FaceImage, Role}
import com.utamars.util.FacePP
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

case class FacialRecognitionApi(implicit ex: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  private val addr = config.getString("http.addr.public")
  private val port = config.getString("http.port")
  private val baseUrl = s"http://$addr:$port/api/assets/face"

  override val route: Route = {
    ((post|put) & path("face"/"recognition") & authnAndAuthz(Role.Assistant)) { acc =>
      uploadedFile("img") { case (metadata, file) => complete(doFacialRecognition(acc, metadata, file))}
    } ~
    (post & path("face") & authnAndAuthz(Role.Assistant)) { acc =>
      uploadedFile("img") { case (metadata, file) => complete(addFaceForRecog(acc, metadata, file))}
    } ~
    (delete & path("face"/Segment) & authnAndAuthz(Role.Admin, Role.Instructor)) { (id, _) =>
      complete(removeFace(id))
    } ~
    (get & path("face") & authnAndAuthz(Role.Assistant)) { acc =>
      complete(getFaceImages(acc.netId))
    } ~
    (get & path("face"/Segment) & authnAndAuthz(Role.Admin, Role.Instructor)) { (netId, _) =>
      complete(getFaceImages(netId))
    } ~
    (get & path("assets"/"face"/Segment)) { id =>
      complete {
        import better.files._
        FaceImage.findBy(id).reply { img =>
          val imgType = id match {
            case ext if ext.contains(".png")  => MediaTypes.`image/png`
            case ext if ext.contains(".jpg")  => MediaTypes.`image/jpeg`
            case ext if ext.contains(".jpeg") => MediaTypes.`image/jpeg`
            case _                            => MediaTypes.`image/pict`
          }

          if (img.path.toFile.exists) HttpResponse(entity = HttpEntity(imgType, img.path.toFile.byteArray))
          else {
            // file no longer exist, clean up database
            img.delete()
            Gone
          }
        }
      }
    }
  }

  private def getFaceImages(netId: String): Future[Response] = {
    case class Image(id: String, url: String)
    implicit val imageFormat = jsonFormat2(Image.apply)

    FaceImage.findAllGood(netId)
      .map(imgs => imgs.map(img => Image(img.id, s"$baseUrl/${img.id}")))
      .reply(imgs => ("images" -> imgs).jsonCompat)
  }

  private def doFacialRecognition(acc: Account, metadata: FileInfo, file: File): Future[Response] = {
    val result = for {
      asst   <- Assistant.findBy(acc.netId).leftMap(err2HttpResp)
      img    <- FaceImage.create(acc.netId, file, metadata, "").leftMap(err2HttpResp)
      faceId <- FacePP.detectionDetect(s"$baseUrl/${img.id}")
      res    <- FacePP.recognitionVerify(s"mars_${acc.netId}", faceId)
      _      <- img.delete().leftMap(err2HttpResp)
    } yield (res._1, res._2, asst.threshold)

    result.reply(
      res => {
        val (confidence, isSamePerson, threshold) = res
          val normalizeConf = if (isSamePerson) confidence / 100.0 else 1 - (confidence / 100.0)
          val json = Map("confidence" -> normalizeConf, "threshold" -> threshold).toJson.compactPrint
          HttpResponse(OK, entity = json)
      },
      errResp => errResp match {
        case HttpResponse(code, _, entity, _) if Seq(403, 431, 432, 500, 502).contains(code.intValue()) =>
          // For issues with the face++ api that are out of our control, just let the
          // recognition request succeed (less annoying for the users).
          // For details of the error codes, see:
          // http://www.faceplusplus.com/detection_detect/ and http://www.faceplusplus.com/recognitionverify-2/
          // todo: email the admin that facial recognition is down?
          logger.error(s">>> Face++ Recognition ISSUE <<<: $code, $entity")
          FaceImage.findAllBad(acc.netId).map(imgs => imgs.foreach(_.delete())) //clean up
          HttpResponse(OK, entity = Map("confidence" -> 100.0, "threshold" -> 0.0).toJson.compactPrint)
        case response              =>
          FaceImage.findAllBad(acc.netId).map(imgs => imgs.foreach(_.delete())) //clean up
          response
      }
    )
  }

  private def addFaceForRecog(acc: Account, metadata: FileInfo, file: File): Future[Response] = {
    val result = for {
      asst     <- Assistant.findBy(acc.netId).leftMap(err2HttpResp)
      img      <- FaceImage.create(acc.netId, file, metadata, "").leftMap(err2HttpResp)
      faceppId <- FacePP.detectionDetect(s"$baseUrl/${img.id}")
      _        <- FacePP.personAddFace(s"mars_${acc.netId}", faceppId)
      _        <- FacePP.trainVerify(s"mars_${acc.netId}")
      _        <- img.copy(faceId = faceppId).update().leftMap(err2HttpResp)
    } yield img.id

    result.reply(
      imgId => (OK, ("url" -> s"$baseUrl/$imgId").jsonCompat),
      resp  => {
        FaceImage.findAllBad(acc.netId).map(imgs => imgs.foreach(_.delete())) //clean up
        resp
      }
    )
  }

  private def removeFace(id: String): Future[Response] = {
    val result = for {
      img <- FaceImage.findBy(id).leftMap(err2HttpResp)
      _   <- FacePP.personRemoveFace(s"mars_${img.netId}", img.faceId)
      _   <- FacePP.trainVerify(s"mars_${img.netId}")
      _   <- img.delete().leftMap(err2HttpResp)
    } yield ()

    result.reply(_ => OK)
  }
}
