package com.utamars.dataaccess

import org.squeryl.Schema

object MySchema extends Schema {

  val accounts = table[Account]
  on(accounts)(acc => declare(
    acc.username is primaryKey
  ))

  val assistants = table[Assistant]
  on(assistants)(a => declare(
    a.employeeId is primaryKey
  ))

  val instructors = table[Instructor]
  on(instructors)(i => declare(
    i.id is primaryKey
  ))

  val clockInOutRecord = table[ClockInOutRecord]
  on(clockInOutRecord)(c => declare(
    c.id is primaryKey
  ))

  val assistantToClockInOutRecord =
    oneToManyRelation(assistants, clockInOutRecord)
      .via((a, c) => a.employeeId === c.employeeId)

  val accountToAssistant =
    oneToManyRelation(accounts, assistants)
    .via((acc, ass) => acc.username === ass.username)

  val accountToInstructor =
    oneToManyRelation(accounts, instructors)
      .via((acc, i) => acc.username === i.username)

  assistantToClockInOutRecord.foreignKeyDeclaration.constrainReference(onDelete cascade)
  accountToAssistant.foreignKeyDeclaration.constrainReference(onDelete cascade)
  accountToInstructor.foreignKeyDeclaration.constrainReference(onDelete cascade)
}
