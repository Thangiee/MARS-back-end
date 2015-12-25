package com.utamars.dataaccess

import cats.data.XorT
import cats.implicits._
import com.utamars.dataaccess.DB.driver.api._
import com.utamars.forms.UpdateAssistantForm

import scala.concurrent.Future

case class Assistant(netId: String, rate: Double, email: String, job: String, department: String,
  lastName: String, firstName: String, employeeId: String, title: String, titleCode: String)

object Assistant {

  def findBy(netId: String): XorT[Future, DataAccessErr, Assistant] =
    withErrHandlingOpt(DB.AssistantTable.filter(_.netId.toLowerCase === netId.toLowerCase).result.headOption)

  def deleteAll(): XorT[Future, DataAccessErr, Unit] = {
    withErrHandling(DBIO.seq(DB.AssistantTable.filter(a => a.netId === a.netId).delete))
  }

  def update(netId: String, form: UpdateAssistantForm): XorT[Future, DataAccessErr, Unit] = {
    findBy(netId).flatMap { asst =>
      asst.copy(
        rate = form.rate.getOrElse(asst.rate),
        department = form.department.getOrElse(asst.department),
        title = form.title.getOrElse(asst.title),
        titleCode = form.titleCode.getOrElse(asst.titleCode)
      ).update()
    }
  }

  implicit class PostfixOps(asst: Assistant) {
    def create(): XorT[Future, DataAccessErr, Unit] = withErrHandling(DBIO.seq(DB.AssistantTable += asst))

    def update(): XorT[Future, DataAccessErr, Unit] =
      withErrHandling(DBIO.seq(DB.AssistantTable.filter(_.netId === asst.netId).update(asst)))
  }
}
