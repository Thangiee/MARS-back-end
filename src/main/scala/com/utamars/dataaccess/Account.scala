package com.utamars.dataaccess

import java.sql.Timestamp

import cats.data.XorT
import cats.implicits._
import com.utamars.dataaccess.DB.driver.api._
import com.utamars.forms.{CreateAssistantForm, CreateInstructorAccForm}

import scala.concurrent.Future

case class Account(netId: String, username: String, passwd: String, role: String, createTime: Timestamp = new Timestamp(System.currentTimeMillis()))

object Account {

  def createFromForm(form: CreateInstructorAccForm): XorT[Future, DataAccessErr, Unit] = withErrHandling {
    DBIO.seq(
      DB.AccountTable += Account(form.netId, form.user, form.pass, Role.Instructor),
      DB.InstructorTable += Instructor(form.netId, form.email, form.lastName, form.firstName)
    ).transactionally
  }

  def createFromForm(form: CreateAssistantForm): XorT[Future, DataAccessErr, Unit] = withErrHandling {
    DBIO.seq(
      DB.AccountTable += Account(form.netId, form.user, form.pass, Role.Assistant),
      DB.AssistantTable += Assistant(form.netId, form.rate, form.email, form.job, form.dept, form.lastName,
                                    form.firstName, form.empId, form.title, form.titleCode)
    ).transactionally
  }

  def findBy(username: String): XorT[Future, DataAccessErr, Account] =
    withErrHandlingOpt(DB.AccountTable.filter(_.username.toLowerCase === username.toLowerCase).result.headOption)

  def add(accs: Account*): XorT[Future, DataAccessErr, Unit] =
    withErrHandling(DBIO.seq(DB.AccountTable ++= accs))

  def deleteBy(username: String): XorT[Future, DataAccessErr, Unit] = findBy(username).flatMap(_.delete())

  def deleteAll(): XorT[Future, DataAccessErr, Unit] = {
    withErrHandling(DBIO.seq(DB.AccountTable.filter(a => a.netId === a.netId).delete))
  }

  def changePassword(username: String, newPass: String): XorT[Future, DataAccessErr, Unit] =
    findBy(username).flatMap(acc => acc.changePassword(newPass))

  implicit class PostfixOps(acc: Account) {
    def create(): XorT[Future, DataAccessErr, Unit] = withErrHandling(DBIO.seq(DB.AccountTable += acc))

    def update(): XorT[Future, DataAccessErr, Unit] =
      withErrHandling(DBIO.seq(DB.AccountTable.filter(_.username === acc.username).update(acc)))

    def changePassword(newPass: String): XorT[Future, DataAccessErr, Unit] =
      acc.copy(passwd = newPass).update()

    def delete(): XorT[Future, DataAccessErr, Unit] =
      withErrHandling(DBIO.seq(DB.AccountTable.filter(_.username === acc.username).delete))
  }
}
