package com.utamars.dataaccess

import java.sql.Timestamp

import cats.data.XorT
import cats.implicits._
import com.github.nscala_time.time.Imports._
import com.utamars.dataaccess.DB.driver.api._
import com.utamars.forms.UpdateRecordForm

import scala.concurrent.Future


case class ClockInOutRecord(
  id: Option[Int],
  netId: String,
  inTime: java.sql.Timestamp = DateTime.now(),
  outTime: Option[java.sql.Timestamp] = None,
  inComputerId: Option[String] = None,
  outComputerId: Option[String] = None
)

object ClockInOutRecord {

  def findBy(netId: String): XorT[Future, DataAccessErr, Seq[ClockInOutRecord]] =
    DB.ClockInOutRecordTable.filter(_.netId.toLowerCase === netId.toLowerCase).sortBy(_.inTime.desc).result

  def findBy(id: Int): XorT[Future, DataAccessErr, ClockInOutRecord] =
    DB.ClockInOutRecordTable.filter(_.id === id).sortBy(_.inTime.desc).result.headOption

  def findMostRecent(netId: String): XorT[Future, DataAccessErr, ClockInOutRecord] =
    DB.ClockInOutRecordTable.filter(_.netId.toLowerCase === netId.toLowerCase).sortBy(_.inTime.desc).result.headOption

  def findBetween(start: LocalDate, end: LocalDate, netId: String): XorT[Future, DataAccessErr, Seq[ClockInOutRecord]] =
    DB.ClockInOutRecordTable.filter(r => r.inTime >= start.toStartOfDayTs && r.inTime <= end.toEndOfDayTs).sortBy(_.inTime.desc).result

  def clockOutAll(netId: String, computerId: Option[String]): XorT[Future, DataAccessErr, Unit] =
    DBIO.seq(
      DB.ClockInOutRecordTable
        .filter(r => r.netId.toLowerCase === netId.toLowerCase && r.outTime.isEmpty)
        .map(r => (r.outTime, r.outComputerId))
        .update((Some(DateTime.now()), computerId))
    ).transactionally

  def update(id: Int, form: UpdateRecordForm): XorT[Future, DataAccessErr, Unit] = {
    findBy(id).flatMap { record =>
      record.copy(
        inTime = if (form.inTime.isDefined) new Timestamp(form.inTime.get) else record.inTime,
        outTime = if (form.outTime.isDefined) Some(new Timestamp(form.outTime.get)) else record.outTime,
        inComputerId = form.inComputerId.orElse(record.inComputerId),
        outComputerId = form.outComputerId.orElse(record.outComputerId)
      ).update()
    }
  }

  def deleteAll(): XorT[Future, DataAccessErr, Unit] = DB.ClockInOutRecordTable.filter(r => r.netId === r.netId).delete

  implicit class PostfixOps(record: ClockInOutRecord) {
    def create(): XorT[Future, DataAccessErr, Int] = DB.ClockInOutRecordTable += record

    def update(): XorT[Future, DataAccessErr, Unit] = DB.ClockInOutRecordTable.filter(_.id === record.id).update(record)
  }
}
