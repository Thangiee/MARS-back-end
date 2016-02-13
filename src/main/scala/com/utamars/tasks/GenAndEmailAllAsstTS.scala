package com.utamars.tasks

import akka.dispatch.ExecutionContexts
import cats.data.Xor
import com.github.nscala_time.time.Imports._
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess._
import com.utamars.util.EMailer

import scala.concurrent.Await
import scala.concurrent.forkjoin.ForkJoinPool

case class GenAndEmailAllAsstTS() extends Runnable with LazyLogging {

  private implicit val executionCtx = ExecutionContexts.fromExecutor(new ForkJoinPool(7))

  override def run(): Unit = {
    logger.info("Task: Generating and emailing all assistant their time sheet for this pay period.")

    Await.result(Assistant.all().value, 1.minute) match {
      case Xor.Right(assts) =>
        val payPeriod = LocalDate.now().halfMonth
        val mails = assts.map { asst =>
          Await.result(TimeSheet.fromDateRange(payPeriod, asst).value, 1.minute)
            .map(ts => (asst.email, ts))
            .leftMap(err => (asst.netId, err))
        }
        mails.par.foreach {
          case Xor.Right((email, ts)) => EMailer.mailTo(email, subject=ts.nameWithoutExtension.replace("_", " "), ts)
          case Xor.Left((netId, err)) => logger.error(s"Unable to email $netId(netID) timesheet because $err")
        }

      case Xor.Left(dataAccessErr) => logger.error(dataAccessErr.toString)
    }
  }

}

