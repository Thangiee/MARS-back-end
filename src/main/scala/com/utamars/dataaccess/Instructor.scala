package com.utamars.dataaccess

import cats.data.XorT
import cats.implicits._
import com.utamars.dataaccess.DB.driver.api._
import com.utamars.forms.UpdateInstructorForm

import scala.concurrent.Future

case class Instructor(netId: String, email: String, lastName: String, firstName: String)

object Instructor {
  private val InstAccTable = for { (inst, acc) <- DB.InstructorTable join DB.AccountTable on (_.netId === _.netId) } yield (inst, acc)

  def all(): DataAccessIO[Seq[Instructor]] = DB.InstructorTable.result

  def allWithAcc(): DataAccessIO[Seq[(Instructor, Account)]] = InstAccTable.result

  def findByNetId(netId: String): DataAccessIO[Instructor] =
    DB.InstructorTable.filter(_.netId.toLowerCase === netId.toLowerCase).result.headOption

  def findByNetIdWithAcc(netId: String): DataAccessIO[(Instructor, Account)] =
    InstAccTable.filter(_._1.netId === netId).result.headOption

  def findByNetIds(netIds: Set[String]): DataAccessIO[Seq[Instructor]] =
    DB.InstructorTable.filter(_.netId inSetBind netIds).result

  def findByNetIdsWithAcc(netIds: Set[String]): DataAccessIO[Seq[(Instructor, Account)]] =
    InstAccTable.filter(_._1.netId inSetBind netIds).result

  def deleteAll(): DataAccessIO[Unit] = DB.InstructorTable.filter(i => i.netId === i.netId).delete

  def update(netId: String, form: UpdateInstructorForm): DataAccessIO[Unit] = {
    findByNetId(netId).flatMap { inst =>
      inst.copy(
        email = form.email.getOrElse(inst.email),
        lastName = form.lastName.getOrElse(inst.lastName),
        firstName = form.firstName.getOrElse(inst.firstName)
      ).update()
    }
  }

  implicit class PostfixOps(instructor: Instructor) {
    def create(): DataAccessIO[Unit] = DB.InstructorTable += instructor

    def update(): DataAccessIO[Unit] =
      DB.InstructorTable.filter(_.netId === instructor.netId).update(instructor)
  }
}
