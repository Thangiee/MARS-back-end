package com.utamars.dataaccess

import java.io.File

import akka.http.scaladsl.server.directives.FileInfo
import cats.data.XorT
import com.utamars.dataaccess.DB.driver.api._
import better.files._

import scala.concurrent.Future
import scala.util.Random

case class FaceImage(id: String, netId: String, faceId: String) {
  def path: String = {
    val dir = config.getString("service.face-recognition.dir")
    s"$dir/$netId/$id"
  }
}

object FaceImage {

  def findBy(id: String): XorT[Future, DataAccessErr, FaceImage] = DB.FaceImageTable.filter(_.id === id).result.headOption

  def findAll(netId: String): XorT[Future, DataAccessErr, Seq[FaceImage]] =
    DB.FaceImageTable.filter(_.netId.toLowerCase === netId.toLowerCase).result

  def deleteBy(id: String): XorT[Future, DataAccessErr, Unit] = DB.FaceImageTable.filter(_.id === id).delete

  def create(netId: String, file: File, metadata: FileInfo, faceId: String): XorT[Future, DataAccessErr, FaceImage] = {
    val f = file.toScala
    val ext = metadata.contentType.mediaType.fileExtensions.headOption.map(ex => "."+ex.replace("jpe", "jpg")).getOrElse("")
    val imgId = Random.alphanumeric.take(5 + Random.nextInt(3)).mkString + ext
    val img = FaceImage(imgId, netId, faceId)

    f.moveTo(img.path.toFile.createIfNotExists(), overwrite = true)
    (DB.FaceImageTable += img).map(_ => img)
  }
}

