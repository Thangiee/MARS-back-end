package com.utamars.dataaccess

import cats.data.XorT
import com.github.nscala_time.time.Imports._
import com.utamars.dataaccess.DB.driver.api._

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
    withErrHandling(DB.ClockInOutRecordTable.filter(_.netId === netId).result)

  def findMostRecent(netId: String): XorT[Future, DataAccessErr, ClockInOutRecord] =
    withErrHandlingOpt(DB.ClockInOutRecordTable.filter(_.netId === netId).sortBy(_.inTime.desc).result.headOption)

  def findBetween(start: LocalDate, end: LocalDate, netId: String) =
    withErrHandling(DB.ClockInOutRecordTable.filter(r => r.inTime >= start.toStartOfDayTs && r.inTime <= end.toEndOfDayTs).result)

  def clockOutAll(netId: String, computerId: String): XorT[Future, DataAccessErr, Unit] = withErrHandling {
    DBIO.seq(
      DB.ClockInOutRecordTable
        .filter(r => r.netId === netId && r.outTime.isEmpty)
        .map(r => (r.outTime, r.outComputerId))
        .update((Some(DateTime.now()), Some(computerId)))
    ).transactionally
  }

  def deleteAll(): XorT[Future, DataAccessErr, Unit] = {
    withErrHandling(DBIO.seq(DB.ClockInOutRecordTable.filter(r => r.netId === r.netId).delete))
  }

  implicit class PostfixOps(record: ClockInOutRecord) {
    def create(): XorT[Future, DataAccessErr, Int] = withErrHandling(DB.ClockInOutRecordTable += record)

    def update(): XorT[Future, DataAccessErr, Int] = {
      withErrHandling(DB.ClockInOutRecordTable.filter(_.id === record.id).update(record))
    }
  }
}
