package com.utamars.api

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.utamars.dataaccess.FaceImage

import scala.concurrent.ExecutionContext

case class AssetsApi(implicit ex: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  override val route: Route = {
    (get & path("assets"/"face"/Segment)) { id =>  // get face image
      complete {
        import better.files._
        FaceImage.findBy(id).reply { img =>
          val imgType = id match {
            case ext if ext.contains(".png")  => MediaTypes.`image/png`
            case ext if ext.contains(".jpg")  => MediaTypes.`image/jpeg`
            case ext if ext.contains(".jpeg") => MediaTypes.`image/jpeg`
            case _                            => MediaTypes.`image/pict`
          }
          if (img.path.toFile.exists) {
            val cacheHeader = headers.`Cache-Control`(public, `max-age`(604800)) // 1 week
            HttpResponse(entity = HttpEntity(imgType, img.path.toFile.byteArray), headers = List(cacheHeader))
          } else {
            // file no longer exist, clean up database
            img.delete()
            Gone
          }
        }
      }
    }
  }
}
