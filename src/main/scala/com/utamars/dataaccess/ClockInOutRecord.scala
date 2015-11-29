package com.utamars.dataaccess

import java.util.{UUID, Date}

import cats.data.Xor

case class ClockInOutRecord(
  employeeId: String,
  computerId: String,
  time: Date,
  clockingIn: Boolean,
  id: UUID = UUID.randomUUID())

object ClockInOutRecord extends Repo {
  override type PK = UUID
  override type T = ClockInOutRecord
  override def table = MySchema.clockInOutRecord

  def findByEmployeeId(id: String): Xor[DataAccessErr, Seq[ClockInOutRecord]] =
    withErrHandling(table.where(record => record.employeeId === id).toSeq)
}
