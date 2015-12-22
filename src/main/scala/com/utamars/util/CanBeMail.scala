package com.utamars.util

import java.io
import javax.mail.internet.InternetAddress

import better.files.File
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import courier._
import simulacrum._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

@typeclass trait CanBeMail[A] {
  def composeContent(a: A): Content
}

object CanBeMail {
  implicit val fileCanBeMail = new CanBeMail[File] {
    override def composeContent(file: File): Content = Multipart().attach(file.toJava)
  }

  implicit val javaFileCanBeMail = new CanBeMail[java.io.File] {
    override def composeContent(file: io.File): Content = Multipart().attach(file)
  }

  implicit val stringCanBeMail = new CanBeMail[String] {
    override def composeContent(txt: String): Content = Text(txt)
  }
}

import CanBeMail.ops._

object EMailer extends AnyRef with LazyLogging {
  private val config = ConfigFactory.load()
  private val envelope = Envelope.from(new InternetAddress(config.getString("email.addr")))
  private val mailer = Mailer(config.getString("email.host"), config.getInt("email.port"))
    .auth(true)
    .as(config.getString("email.SMTP-user"), config.getString("email.SMTP-password"))
    .startTtls(true)()

  def mailTo[T: CanBeMail](addr: String, subject: String = "", canBeMail: T)(implicit ec: ExecutionContext): Unit = {
    val sendMail = mailer(envelope.to(new InternetAddress(addr))
      .subject(subject)
      .content(canBeMail.composeContent)
    )

    sendMail.onComplete {
      case Success(_) => logger.info(s"Successfully sent $subject e-mail to $addr")
      case Failure(e) => logger.error(e.getMessage, e.getCause)
    }
  }
}


