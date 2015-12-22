package com.utamars.dataaccess

import cats.data.XorT
import com.utamars.dataaccess.DB.driver.api._

import scala.concurrent.Future

case class Assistant(netId: String, rate: Double, email: String, job: String, department: String,
  lastName: String, firstName: String, employeeId: String, title: String, titleCode: String)

object Assistant {

  def findBy(netId: String): XorT[Future, DataAccessErr, Assistant] =
    withErrHandlingOpt(DB.AssistantTable.filter(_.netId.toLowerCase === netId.toLowerCase).result.headOption)

  def deleteAll(): XorT[Future, DataAccessErr, Unit] = {
    withErrHandling(DBIO.seq(DB.AssistantTable.filter(a => a.netId === a.netId).delete))
  }

  implicit class PostfixOps(asst: Assistant) {
    def create(): XorT[Future, DataAccessErr, Unit] = withErrHandling(DBIO.seq(DB.AssistantTable += asst))
  }
}
