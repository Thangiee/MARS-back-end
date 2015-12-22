package com.utamars.util

import java.sql.Timestamp

import com.github.nscala_time.time.DurationBuilder
import com.github.nscala_time.time.Imports._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.implicitConversions

trait TimeConversion {
  // convert nscala-time to scala.concurrent.duration when necessary
  implicit def concurrentFiniteDurationFrom(d: DurationBuilder): FiniteDuration =
    Duration( d.millis, scala.concurrent.duration.MILLISECONDS)

  implicit def jodaDateTimeFrom(ts: Timestamp): DateTime = new DateTime(ts.getTime)

  implicit def timeStampFrom(dateTime: DateTime): Timestamp = new Timestamp(dateTime.getMillis)

  implicit def jodaLocalDateFrom(ts: Timestamp): LocalDate = new LocalDate(ts.getTime)

  implicit class LocalDateConversion(localDate: LocalDate) {
    def toStartOfDayTs: Timestamp = new Timestamp(localDate.toDateTimeAtStartOfDay.getMillis)
    def toEndOfDayTs: Timestamp = new Timestamp((localDate.toDateTimeAtStartOfDay + 1.day - 1.milli).getMillis)
  }
}

object TimeConversion extends TimeConversion