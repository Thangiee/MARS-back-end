package com.utamars.dataaccess

import java.sql.Timestamp

import cats.data.XorT
import cats.implicits._
import com.utamars.dataaccess.DB.driver.api._
import com.utamars.forms.{CreateAssistantForm, CreateInstructorAccForm}

import scala.concurrent.Future

case class Account(
  netId: String,
  username: String,
  passwd: String,
  role: String,
  createTime: Timestamp = new Timestamp(System.currentTimeMillis()),
  approve: Boolean
)

object Account {

  def all(): XorT[Future, DataAccessErr, Seq[Account]] = DB.AccountTable.result

  def createFromForm(form: CreateInstructorAccForm): XorT[Future, DataAccessErr, Unit] =
    DBIO.seq(
      DB.AccountTable += Account(form.netId, form.user, form.pass, Role.Instructor, approve = true),
      DB.InstructorTable += Instructor(form.netId, form.email, form.lastName, form.firstName)
    ).transactionally

  def createFromForm(form: CreateAssistantForm): XorT[Future, DataAccessErr, Unit] =
    DBIO.seq(
      DB.AccountTable += Account(form.netId, form.user, form.pass, Role.Assistant, approve = false),
      DB.AssistantTable += Assistant(form.netId, form.rate, form.email, form.job, form.dept, form.lastName,
        form.firstName, form.empId, form.title, form.titleCode, form.threshold.getOrElse(.4))
    ).transactionally

  def findByUsername(username: String): XorT[Future, DataAccessErr, Account] =
    DB.AccountTable.filter(_.username.toLowerCase === username.toLowerCase).result.headOption

  def findByNetIds(netIds: Set[String]): XorT[Future, DataAccessErr, Seq[Account]] =
    DB.AccountTable.filter(_.netId inSetBind netIds).result

  def findByNetId(netId: String): XorT[Future, DataAccessErr, Account] =
    DB.AccountTable.filter(_.netId === netId).result.headOption

  def add(accs: Account*): XorT[Future, DataAccessErr, Unit] = DB.AccountTable ++= accs

  def deleteByUsername(username: String): XorT[Future, DataAccessErr, Unit] = findByUsername(username).flatMap(_.delete())

  def deleteAll(): XorT[Future, DataAccessErr, Unit] = DB.AccountTable.filter(a => a.netId === a.netId).delete

  def changePassword(username: String, newPass: String): XorT[Future, DataAccessErr, Account] =
    findByUsername(username).flatMap(acc => acc.changePassword(newPass))

  implicit class PostfixOps(acc: Account) {
    def create(): XorT[Future, DataAccessErr, Unit] = DB.AccountTable += acc

    def update(): XorT[Future, DataAccessErr, Account] = DB.AccountTable.filter(_.username === acc.username).update(acc).map(_ => acc)

    def changePassword(newPass: String): XorT[Future, DataAccessErr, Account] = acc.copy(passwd = newPass).update()

    def delete(): XorT[Future, DataAccessErr, Unit] = DB.AccountTable.filter(_.username === acc.username).delete
  }
}