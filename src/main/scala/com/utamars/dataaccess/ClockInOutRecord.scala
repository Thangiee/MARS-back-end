package com.utamars.dataaccess

import java.sql.Timestamp

import cats.implicits._
import com.github.nscala_time.time.Imports._
import com.utamars.dataaccess.DB.driver.api._
import com.utamars.forms.UpdateRecordForm


case class ClockInOutRecord(
  id: Option[Int],
  netId: String,
  inTime: java.sql.Timestamp = DateTime.now(),
  outTime: Option[java.sql.Timestamp] = None,
  inComputerId: Option[String] = None,
  outComputerId: Option[String] = None
)

object ClockInOutRecord {

  def all(): DataAccessIO[Seq[ClockInOutRecord]] = DB.ClockInOutRecordTable.sortBy(_.inTime.desc).result

  def findByNetId(netId: String): DataAccessIO[Seq[ClockInOutRecord]] =
    DB.ClockInOutRecordTable.filter(_.netId.toLowerCase === netId.toLowerCase).sortBy(_.inTime.desc).result

  def findById(id: Int): DataAccessIO[ClockInOutRecord] =
    DB.ClockInOutRecordTable.filter(_.id === id).sortBy(_.inTime.desc).result.headOption

  def findMostRecent(netId: String): DataAccessIO[ClockInOutRecord] =
    DB.ClockInOutRecordTable.filter(_.netId.toLowerCase === netId.toLowerCase).sortBy(_.inTime.desc).result.headOption

  def findBetween(start: LocalDate, end: LocalDate, netId: String, inclusive: Boolean=true): DataAccessIO[Seq[ClockInOutRecord]] =
    DB.ClockInOutRecordTable
      .filter(r => r.netId.toLowerCase === netId.toLowerCase &&
                   r.inTime >= start.toStartOfDayTimestamp &&
                   r.outTime.map(_ <= end.toEndOfDayTimestamp).getOrElse(inclusive)) // if inclusive is false, record with no out time will be excluded
      .sortBy(_.inTime.desc).result

  def clockOutAll(netId: String, computerId: Option[String]): DataAccessIO[Unit] =
    DBIO.seq(
      DB.ClockInOutRecordTable
        .filter(r => r.netId.toLowerCase === netId.toLowerCase && r.outTime.isEmpty)
        .map(r => (r.outTime, r.outComputerId))
        .update((Some(DateTime.now()), computerId))
    ).transactionally

  def clockOutAll(computerId: Option[String]): DataAccessIO[Seq[String]] = {
    val records = DB.ClockInOutRecordTable.filter(r => r.outTime.isEmpty)
    (for {
      y <- records.map(_.netId).result
      x <- records.map(r => (r.outTime, r.outComputerId)).update((Some(DateTime.now()), computerId))
    } yield y).transactionally
  }

  def update(id: Int, form: UpdateRecordForm): DataAccessIO[Unit] = {
    findById(id).flatMap { record =>
      record.copy(
        inTime = if (form.inTime.isDefined) new Timestamp(form.inTime.get) else record.inTime,
        outTime = if (form.outTime.isDefined) Some(new Timestamp(form.outTime.get)) else record.outTime,
        inComputerId = form.inComputerId.orElse(record.inComputerId),
        outComputerId = form.outComputerId.orElse(record.outComputerId)
      ).update()
    }
  }

  def deleteById(id: Int): DataAccessIO[Unit] = DB.ClockInOutRecordTable.filter(_.id === id).delete

  def deleteAll(): DataAccessIO[Unit] = DB.ClockInOutRecordTable.filter(r => r.netId === r.netId).delete

  implicit class PostfixOps(record: ClockInOutRecord) {
    def create(): DataAccessIO[Int] = DB.ClockInOutRecordTable += record

    def update(): DataAccessIO[Unit] = DB.ClockInOutRecordTable.filter(_.id === record.id).update(record)
  }
}
