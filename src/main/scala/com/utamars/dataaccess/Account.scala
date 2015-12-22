package com.utamars.dataaccess

import java.sql.Timestamp

import cats.data.XorT
import com.utamars.dataaccess.DB.driver.api._

import scala.concurrent.Future

case class Account(netId: String, username: String, passwd: String, role: String, lastLogin: Timestamp = new Timestamp(System.currentTimeMillis()))

object Account {

  def findBy(username: String): XorT[Future, DataAccessErr, Account] =
    withErrHandlingOpt(DB.AccountTable.filter(_.username.toLowerCase === username.toLowerCase).result.headOption)

  def add(accs: Account*): XorT[Future, DataAccessErr, Unit] =
    withErrHandling(DBIO.seq(DB.AccountTable ++= accs))

  def deleteAll(): XorT[Future, DataAccessErr, Unit] = {
    withErrHandling(DBIO.seq(DB.AccountTable.filter(a => a.netId === a.netId).delete))
  }

  implicit class PostfixOps(acc: Account) {
    def create(): XorT[Future, DataAccessErr, Unit] = withErrHandling(DBIO.seq(DB.AccountTable += acc))
  }
}
