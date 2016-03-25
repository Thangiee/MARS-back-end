package com.utamars

import com.typesafe.config.ConfigFactory

package object util {

  trait Implicits extends AnyRef with TimeImplicits with JsonImplicits
  object Implicits extends Implicits

  object Config {
    val config = ConfigFactory.load()

    val privateAddr   = config.getString("http.addr.private")
    val publicAddr    = config.getString("http.addr.public")
    val port          = config.getInt("http.port")
    val publicUrl     = s"http://$publicAddr:$port"
    val createSchema  = config.getBoolean("db.create")
    val dbParallelism = config.getInt("db.parallelism")
    val faceImgDir    = config.getString("service.face-recognition.dir")
    val timeSheetDir  = config.getString("service.timesheet.dir")
    val faceppSecret  = config.getString("facepp.secret")
    val faceppKey     = config.getString("facepp.key")
    val emailAddr     = config.getString("email.addr")
    val emailHost     = config.getString("email.host")
    val emailPort     = config.getInt("email.port")
    val STMPUser      = config.getString("email.SMTP-user")
    val STMPPasswd    = config.getString("email.SMTP-password")
    val uuidTTL       = config.getInt("service.registerUUID.ttl-in-sec")
  }

  /* face_image.id, NOT face_img.face_id */
  def mkFaceImgAssetUrl(id: String): String = Config.publicUrl + "/api/assets/face/" +id
  def mkFaceImgAssetUrl(id: Option[String]): String = id.map(mkFaceImgAssetUrl(_)).getOrElse("")


  implicit class IntOps(i: Int) {
    def between(a: Int, b: Int): Boolean = a to b contains i
  }

}
