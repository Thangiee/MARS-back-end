package com.utamars.dataaccess

import java.sql.Timestamp

import cats.implicits._
import com.utamars.dataaccess.DB.driver.api._
import com.utamars.forms.{CreateAssistantForm, CreateInstructorAccForm}


case class Account(
  netId: String,
  username: String,
  passwd: String,
  role: String,
  createTime: Timestamp = new Timestamp(System.currentTimeMillis()),
  approve: Boolean
)

object Account {

  def all(): DataAccessIO[Seq[Account]] = DB.AccountTable.result

  def createFromForm(form: CreateInstructorAccForm): DataAccessIO[Unit] =
    DBIO.seq(
      DB.AccountTable += Account(form.netId, form.user, form.pass, Role.Instructor, approve = false),
      DB.InstructorTable += Instructor(form.netId, form.email, form.lastName, form.firstName)
    ).transactionally

  def createFromForm(form: CreateAssistantForm): DataAccessIO[Unit] =
    DBIO.seq(
      DB.AccountTable += Account(form.netId, form.user, form.pass, Role.Assistant, approve = false),
      DB.AssistantTable += Assistant(form.netId, form.rate, form.email, form.job, form.dept, form.lastName,
        form.firstName, form.empId, form.title, form.titleCode, form.threshold.getOrElse(.4))
    ).transactionally

  def findByUsername(username: String): DataAccessIO[Account] =
    DB.AccountTable.filter(_.username.toLowerCase === username.toLowerCase).result.headOption

  def findByNetIds(netIds: Set[String]): DataAccessIO[Seq[Account]] =
    DB.AccountTable.filter(_.netId inSetBind netIds).result

  def findByNetId(netId: String): DataAccessIO[Account] =
    DB.AccountTable.filter(_.netId === netId).result.headOption

  def add(accs: Account*): DataAccessIO[Unit] = DB.AccountTable ++= accs

  def deleteByUsername(username: String): DataAccessIO[Unit] = findByUsername(username).flatMap(_.delete())

  def deleteAll(): DataAccessIO[Unit] = DB.AccountTable.filter(a => a.netId === a.netId).delete

  def changePassword(netId: String, newPass: String): DataAccessIO[Account] =
    findByNetId(netId).flatMap(acc => acc.changePassword(newPass))

  def findEmailByNetId(netId: String): DataAccessIO[String] = {
    val q1 = DB.AssistantTable.filter(_.netId === netId).map(_.email)
    val q2 = DB.InstructorTable.filter(_.netId === netId).map(_.email)
    (q1 union q2).result.headOption
  }

  implicit class PostfixOps(acc: Account) {
    def create(): DataAccessIO[Unit] = DB.AccountTable += acc

    def update(): DataAccessIO[Account] = DB.AccountTable.filter(_.username === acc.username).update(acc).map(_ => acc)

    def changePassword(newPass: String): DataAccessIO[Account] = acc.copy(passwd = newPass).update()

    def delete(): DataAccessIO[Unit] = DB.AccountTable.filter(_.username === acc.username).delete
  }
}