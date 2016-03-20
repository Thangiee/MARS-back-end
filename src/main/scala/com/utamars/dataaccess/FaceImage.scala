package com.utamars.dataaccess

import java.io.File

import akka.http.scaladsl.server.directives.FileInfo
import better.files._
import com.utamars.dataaccess.DB.driver.api._

import scala.util.{Random, Try}

case class FaceImage(id: String, netId: String, faceId: String) {
  def path: String = {
    val dir = com.utamars.util.Config.faceImgDir
    s"$dir/$netId/$id"
  }
}

object FaceImage {

  def findById(id: String): DataAccessIO[FaceImage] = DB.FaceImageTable.filter(_.id === id).result.headOption

  def findAllGood(netId: String): DataAccessIO[Seq[FaceImage]] =
    DB.FaceImageTable.filter(r => r.netId.toLowerCase === netId.toLowerCase && r.faceId =!= "").result

  def findAllBad(netId: String): DataAccessIO[Seq[FaceImage]] =
    DB.FaceImageTable.filter(r => r.netId.toLowerCase === netId.toLowerCase && r.faceId === "").result

  def create(netId: String, file: File, metadata: FileInfo, faceId: String): DataAccessIO[FaceImage] = {
    val f = file.toScala
    val ext = metadata.contentType.mediaType.subType
    val imgId = Random.alphanumeric.take(5 + Random.nextInt(3)).mkString + "." + ext
    val img = FaceImage(imgId, netId, faceId)

    f.moveTo(img.path.toFile.createIfNotExists(), overwrite = true)
    (DB.FaceImageTable += img).map(_ => img)
  }

  implicit class PostfixOps(faceImage: FaceImage) {
    def update(): DataAccessIO[Unit] =
      DB.FaceImageTable.filter(_.id === faceImage.id).update(faceImage)

    def delete(): DataAccessIO[Unit] = {
      Try(faceImage.path.toFile.delete(ignoreIOExceptions = true))
      DB.FaceImageTable.filter(_.id === faceImage.id).delete
    }
  }
}

