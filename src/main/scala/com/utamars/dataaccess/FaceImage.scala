package com.utamars.dataaccess

import java.io.File

import akka.http.scaladsl.server.directives.FileInfo
import cats.data.XorT
import com.utamars.dataaccess.DB.driver.api._
import better.files._

import scala.concurrent.Future
import scala.util.{Try, Random}

case class FaceImage(id: String, netId: String, faceId: String) {
  def path: String = {
    val dir = config.getString("service.face-recognition.dir")
    s"$dir/$netId/$id"
  }
}

object FaceImage {

  def findById(id: String): XorT[Future, DataAccessErr, FaceImage] = DB.FaceImageTable.filter(_.id === id).result.headOption

  def findAllGood(netId: String): XorT[Future, DataAccessErr, Seq[FaceImage]] =
    DB.FaceImageTable.filter(r => r.netId.toLowerCase === netId.toLowerCase && r.faceId =!= "").result

  def findAllBad(netId: String): XorT[Future, DataAccessErr, Seq[FaceImage]] =
    DB.FaceImageTable.filter(r => r.netId.toLowerCase === netId.toLowerCase && r.faceId === "").result

  def create(netId: String, file: File, metadata: FileInfo, faceId: String): XorT[Future, DataAccessErr, FaceImage] = {
    val f = file.toScala
    val ext = metadata.contentType.mediaType.subType
    val imgId = Random.alphanumeric.take(5 + Random.nextInt(3)).mkString + "." + ext
    val img = FaceImage(imgId, netId, faceId)

    f.moveTo(img.path.toFile.createIfNotExists(), overwrite = true)
    (DB.FaceImageTable += img).map(_ => img)
  }

  implicit class PostfixOps(faceImage: FaceImage) {
    def update(): XorT[Future, DataAccessErr, Unit] =
      DB.FaceImageTable.filter(_.id === faceImage.id).update(faceImage)

    def delete(): XorT[Future, DataAccessErr, Unit] = {
      Try(faceImage.path.toFile.delete(ignoreIOExceptions = true))
      DB.FaceImageTable.filter(_.id === faceImage.id).delete
    }
  }
}

