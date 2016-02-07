package com.utamars.util

import java.sql.Timestamp

import com.github.nscala_time.time.DurationBuilder
import com.github.nscala_time.time.Imports._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.implicitConversions

trait TimeImplicits {
  // convert nscala-time to scala.concurrent.duration when necessary
  implicit def concurrentFiniteDurationFrom(d: DurationBuilder): FiniteDuration =
    Duration( d.millis, scala.concurrent.duration.MILLISECONDS)

  implicit def timestamp2DateTime(ts: Timestamp): DateTime = new DateTime(ts.getTime)

  implicit def dateTime2Timestamp(dateTime: DateTime): Timestamp = new Timestamp(dateTime.getMillis)

  implicit def timestamp2localDate(ts: Timestamp): LocalDate = new LocalDate(ts.getTime)

  implicit class LocalDateOps(localDate: LocalDate) {
    def toStartOfDayTimestamp: Timestamp = new Timestamp(localDate.toDateTimeAtStartOfDay.getMillis)
    def toEndOfDayTimestamp: Timestamp = new Timestamp((localDate.toDateTimeAtStartOfDay + 1.day - 1.milli).getMillis)

    def halfMonth: (LocalDate, LocalDate) = {
      val y = localDate.getYear
      val m = localDate.getMonthOfYear

      if (localDate.getDayOfMonth < 16) {  // first half of the month; day 1 to 15.
      val start = new LocalDate(y, m, 1)
        val end   = new LocalDate(y, m, 15)
        (start, end)
      } else {      // second half of the month; day 16 to last day of the month
      val start = new LocalDate(y, m, 16)
        val end   = new LocalDate(y, m, 28).dayOfMonth().withMaximumValue()
        (start, end)
      }
    }
  }

  implicit class DateTimeOps(dateTime: DateTime) {
    def roundToQuarterHr: DateTime = {
      dateTime.getMinuteOfHour match {
        case m if m between(0, 7)   => dateTime.withMinute(0).withSecond(0)
        case m if m between(8, 22)  => dateTime.withMinute(15).withSecond(0)
        case m if m between(23, 37) => dateTime.withMinute(30).withSecond(0)
        case m if m between(38, 52) => dateTime.withMinute(45).withSecond(0)
        case _ /* 53 to 59 */       => dateTime.withMinute(0).withSecond(0) + 1.hour
      }
    }
  }
}

object TimeImplicits extends TimeImplicits