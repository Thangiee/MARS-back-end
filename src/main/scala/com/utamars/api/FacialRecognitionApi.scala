package com.utamars.api


import java.io.File

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import cats.data.Xor
import cats.std.all._
import com.facepp.error.FaceppParseException
import com.facepp.http.PostParameters
import com.utamars.dataaccess.{Account, Assistant, FaceImage, Role}
import spray.json._

import scala.concurrent.ExecutionContext

case class FacialRecognitionApi(implicit ex: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  private val addr = config.getString("http.addr.public")
  private val port = config.getString("http.port")
  private val baseUrl = s"http://$addr:$port/api/assets/face"

  override val route: Route = {
    ((post|put) & path("face"/"recognition") & authnAndAuthz(Role.Assistant)) { acc =>
      uploadedFile("img") { case (metadata, file) => doFacialRecognition(acc, file) }
    } ~
    (post & path("face") & authnAndAuthz(Role.Assistant)) { acc =>
      uploadedFile("img") { case (metadata, file) => addFace(acc, file, metadata) }
    } ~
    (delete & path("face"/Segment) & authnAndAuthz(Role.Admin, Role.Instructor)) { (id, _) =>
      removeFace(id)
    } ~
    (get & path("face") & authnAndAuthz(Role.Assistant)) { acc =>
      getFaceImages(acc.netId)
    } ~
    (get & path("face"/Segment) & authnAndAuthz(Role.Admin, Role.Instructor)) { (netId, _) =>
      getFaceImages(netId)
    } ~
    (get & path("assets"/"face"/Segment)) { id =>
      import better.files._
      FaceImage.findBy(id).responseWith { img =>
        val imgType = id match {
          case ext if ext.contains(".png")  => MediaTypes.`image/png`
          case ext if ext.contains(".jpg")  => MediaTypes.`image/jpeg`
          case ext if ext.contains(".jpeg") => MediaTypes.`image/jpeg`
          case _                            => MediaTypes.`image/pict`
        }

        if (img.path.toFile.exists) HttpResponse(entity = HttpEntity(imgType, img.path.toFile.byteArray))
        else {
          // file no longer exist, clean up database
          FaceImage.deleteBy(id)
          Gone
        }
      }
    }
  }

  private def getFaceImages(netId: String): Route = {
    case class Image(id: String, url: String)
    implicit val imageFormat = jsonFormat2(Image.apply)

    FaceImage.findAll(netId)
      .map(imgs => imgs.map(img => Image(img.id, s"$baseUrl/${img.id}")))
      .responseWith(imgs => Map("images" -> imgs).toJson.compactPrint)
  }

  private def doFacialRecognition(acc: Account, file: File): Route = {
    Assistant.findBy(acc.netId).responseWith { asst =>
      val result = for {
        json   <- Xor.catchNonFatal(facePlusPlus.detectionDetect(new PostParameters().setImg(file)))
        _       = println(json)
        faceId <- Xor.catchNonFatal(json.getJSONArray("face").getJSONObject(0).getString("face_id"))
        param   = new PostParameters().setPersonName(s"mars_${acc.netId}").setFaceId(faceId)
        result <- Xor.catchNonFatal(facePlusPlus.recognitionVerify(param))
        confidence   = result.getDouble("confidence")
        isSamePerson = result.getBoolean("is_same_person")
      } yield (confidence, isSamePerson)

      result.fold(
        {
          case err@(ex: FaceppParseException) =>
            if (Seq(403, 431, 432, 500, 502).contains(ex.code)) {
              // For issues with the face++ api that are out of our control, just let the
              // recognition request succeed (less annoying for the users).
              // For details of the error codes, see:
              // http://www.faceplusplus.com/detection_detect/ and http://www.faceplusplus.com/recognitionverify-2/
              // todo: email the admin that facial recognition is down?
              logger.error(s">>> Face++ Recognition ISSUE <<<: ${ex.code}, ${ex.msg}", ex)
              HttpResponse(OK)
            } else {
              faceppErrHandler(err)
            }
        },
        res => {
          val (confidence, isSamePerson) = res
          val normalizeConf = if (isSamePerson) confidence / 100.0 else 1 - (confidence / 100.0)
          val json = Map("confidence" -> normalizeConf, "threshold" -> asst.threshold).toJson.compactPrint
          HttpResponse(OK, entity = json)
        }
      )
    }
  }

  private def addFace(acc: Account, file: File, metadata: FileInfo): Route = {
    val result = for {
      json   <- Xor.catchNonFatal(facePlusPlus.detectionDetect(new PostParameters().setImg(file)))
      faceId <- Xor.catchNonFatal(json.getJSONArray("face").getJSONObject(0).getString("face_id"))
      param   = new PostParameters().setPersonName(s"mars_${acc.netId}").setFaceId(faceId)
      _      <- Xor.catchNonFatal(facePlusPlus.personAddFace(param))
      _      <- Xor.catchNonFatal(facePlusPlus.trainVerify(new PostParameters().setPersonName(s"mars_${acc.netId}")))
    } yield faceId

    result.fold(
      err => complete(faceppErrHandler(err)),
      faceId => FaceImage.create(acc.netId, file, metadata, faceId).responseWith(img => Map("url" -> s"$baseUrl/${img.id}").toJson.compactPrint)
    )
  }

  private def removeFace(id: String): Route = {
    FaceImage.findBy(id).responseWith { img =>
      val result = for {
        x <- Xor.catchNonFatal(facePlusPlus.personRemoveFace(new PostParameters().setPersonName(s"mars_${img.netId}").setFaceId(img.faceId)))
        _ <- Xor.catchNonFatal(facePlusPlus.trainVerify(new PostParameters().setPersonName(s"mars_${img.netId}")))
      } yield ()

      result.fold(
        err  => faceppErrHandler(err),
        succ => { FaceImage.deleteBy(id); HttpResponse(OK) }
      )
    }
  }
}
