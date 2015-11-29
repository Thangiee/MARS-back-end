package com.utamars.dataaccess

import java.util.Date

import cats.data.Xor
import org.squeryl._

case class Account(username: String, passwd: String, role: String, lastLogin: Date = new Date())

object Account extends Repo {
  override type PK = String
  override type T = Account
  override val table: Table[Account] = MySchema.accounts

  def accountsByRoles(roles: Role*): Xor[DataAccessErr, Seq[Account]] = withErrHandling {
    table.where(acc => acc.role in roles).distinct.toSeq
  }

  implicit class Ops(acc: Account) {
    def changePasswd(newPasswd: String): Xor[DataAccessErr, Account] =
      for (updatedAcc <- update(acc.copy(passwd = newPasswd))) yield updatedAcc
  }
}
