package com.utamars.dataaccess

import org.squeryl.Schema

object MySchema extends Schema {

  val accounts = table[Account]
  on(accounts)(acc => declare(
    acc.username is primaryKey
  ))

  val assistants = table[Assistant]
  on(assistants)(asst => declare(
    asst.employeeId is primaryKey,
    asst.email is unique,
    asst.albumName is unique,
    asst.albumKey is unique
  ))

  val instructors = table[Instructor]
  on(instructors)(inst => declare(
    inst.id is primaryKey,
    inst.email is unique
  ))

  val clockInOutRecord = table[ClockInOutRecord]
  on(clockInOutRecord)(record => declare(
    record.id is primaryKey
  ))

  val assistantToClockInOutRecord =
    oneToManyRelation(assistants, clockInOutRecord)
      .via((asst, record) => asst.employeeId === record.employeeId)

  val accountToAssistant =
    oneToManyRelation(accounts, assistants)
    .via((acc, asst) => acc.username === asst.username)

  val accountToInstructor =
    oneToManyRelation(accounts, instructors)
      .via((acc, inst) => acc.username === inst.username)

  assistantToClockInOutRecord.foreignKeyDeclaration.constrainReference(onDelete cascade)
  accountToAssistant.foreignKeyDeclaration.constrainReference(onDelete cascade)
  accountToInstructor.foreignKeyDeclaration.constrainReference(onDelete cascade)
}
