package com.utamars.api

import akka.http.scaladsl.model.MediaType.Binary
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import better.files.File
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.ScaleMethod.Bicubic
import com.sksamuel.scrimage.nio.JpegWriter
import com.utamars.dataaccess.FaceImage
import com.utamars.util.IntOps

import scala.concurrent.ExecutionContext

case class AssetsApi(implicit ex: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  override val route: Route = {
    (get & path("assets"/"face"/Segment) & parameter('size.as[Int].?)) { (id, size) =>  // get face image
      import better.files._
      complete(FaceImage.findById(id).reply { img =>
        val imgFile = img.path.toFile
        if (imgFile.exists) {
          val cacheHeader = headers.`Cache-Control`(public, `max-age`(604800)) // 1 week
          size match {
            case Some(s) =>
              if (s.between(3, 512)) HttpResponse(entity = HttpEntity(MediaTypes.`image/jpeg`, resize(s, s, imgFile)), headers = List(cacheHeader))
              else                   HttpResponse(BadRequest, entity = "Size must be between 3 and 512.")
            case None    =>          HttpResponse(entity = HttpEntity(imgType(id), imgFile.byteArray), headers = List(cacheHeader))
          }
        } else {
          // file no longer exist, clean up database
          img.delete()
          Gone
        }
      })
    }
  }

  private def resize(w: Int, h: Int, img: File): Array[Byte] = Image.fromFile(img.toJava).cover(w, h, Bicubic).bytes(JpegWriter())

  private def imgType(name: String): Binary = name match {
    case ext if ext.contains(".png")  => MediaTypes.`image/png`
    case ext if ext.contains(".jpg")  => MediaTypes.`image/jpeg`
    case ext if ext.contains(".jpeg") => MediaTypes.`image/jpeg`
    case _                            => MediaTypes.`image/pict`
  }
}
