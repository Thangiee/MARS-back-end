package com.utamars.tasks

import akka.dispatch.ExecutionContexts
import cats.data.Xor
import com.typesafe.scalalogging.LazyLogging
import com.github.nscala_time.time.Imports._
import com.utamars.dataaccess._
import com.utamars.util.EMailer
import org.joda.time.format.DateTimeFormat

import scala.concurrent.Await
import scala.concurrent.forkjoin.ForkJoinPool

case class ClockOutAndNotify() extends Runnable with LazyLogging {

  private implicit val executionCtx = ExecutionContexts.fromExecutor(new ForkJoinPool(7))

  override def run(): Unit = {
    logger.info("Task: Clock out assistants that forgot to clock out and notify them.")

    val outTime = DateTimeFormat.forPattern("h:mm a, MMM dd").print(DateTime.now())
    val subject = "Did you forget to clock out?"
    val message =
      s"""It appears that you forgot to clock out so the system did it for you at approximately $outTime.
         |In any case, please see the administrator to get the clock out time corrected.
      """.stripMargin

    Await.result(ClockInOutRecord.clockOutAll(Some("Forgot to clock out")).value, 1.minute) match {
      case Xor.Right(netIds) =>
        val mails = netIds.map { id =>
          Await.result(Assistant.findByNetId(id).value, 1.minute).map(_.email).leftMap(err => (id, err))
        }
        mails.par.foreach {
          case Xor.Right(email)       => EMailer.mailTo(email, subject, message)
          case Xor.Left((netId, err)) => logger.error(s"Unable to notify $netId(netID) because $err")
        }

      case Xor.Left(dataAccessErr) => logger.error(dataAccessErr.toString)
    }
  }

}
