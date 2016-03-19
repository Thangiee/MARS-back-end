package com.utamars.util

import java.io
import javax.activation.{MailcapCommandMap, CommandMap}
import javax.mail.internet.InternetAddress

import better.files.File
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
  // http://stackoverflow.com/a/25650033
  private val mc = CommandMap.getDefaultCommandMap.asInstanceOf[MailcapCommandMap]
  mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
  mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
  mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
  mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
  mc.addMailcap("message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822")

  private val envelope = Envelope.from(new InternetAddress(Config.emailAddr))
  private val mailer = Mailer(Config.emailHost, Config.emailPort)
    .auth(true)
    .as(Config.STMPUser, Config.STMPPasswd)
    .startTtls(true)()

  def mailTo[T: CanBeMail](addr: String, subject: String = "", canBeMail: T)(implicit ec: ExecutionContext): Unit = {
    val sendMail = mailer(envelope.to(new InternetAddress(addr))
      .subject(subject)
      .content(canBeMail.composeContent)
    )

    sendMail.onComplete {
      case Success(_) => logger.info(s"Successfully sent '$subject' e-mail to $addr")
      case Failure(e) => logger.error(e.getMessage, e.getCause)
    }
  }
}


