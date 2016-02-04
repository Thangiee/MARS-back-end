package com.utamars.dataaccess

import cats.data.XorT
import cats.implicits._
import com.utamars.dataaccess.DB.driver.api._
import com.utamars.forms.UpdateInstructorForm

import scala.concurrent.Future

case class Instructor(netId: String, email: String, lastName: String, firstName: String)

object Instructor {

  def all(): XorT[Future, DataAccessErr, Seq[Instructor]] = DB.InstructorTable.result

  def findBy(netId: String): XorT[Future, DataAccessErr, Instructor] =
    DB.InstructorTable.filter(_.netId.toLowerCase === netId.toLowerCase).result.headOption

  def deleteAll(): XorT[Future, DataAccessErr, Unit] = DB.InstructorTable.filter(i => i.netId === i.netId).delete

  def update(netId: String, form: UpdateInstructorForm): XorT[Future, DataAccessErr, Unit] = {
    findBy(netId).flatMap { inst =>
      inst.copy(
        email = form.email.getOrElse(inst.email),
        lastName = form.lastName.getOrElse(inst.lastName),
        firstName = form.firstName.getOrElse(inst.firstName)
      ).update()
    }
  }

  implicit class PostfixOps(instructor: Instructor) {
    def create(): XorT[Future, DataAccessErr, Unit] = DB.InstructorTable += instructor

    def update(): XorT[Future, DataAccessErr, Unit] =
      DB.InstructorTable.filter(_.netId === instructor.netId).update(instructor)
  }
}
