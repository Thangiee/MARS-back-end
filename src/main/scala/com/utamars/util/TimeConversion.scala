package com.utamars.util

import java.sql.Timestamp

import com.github.nscala_time.time.DurationBuilder
import org.joda.time.DateTime

import scala.concurrent.duration.{Duration, FiniteDuration}

trait TimeConversion {
  // convert nscala-time to scala.concurrent.duration when necessary
  implicit def concurrentFiniteDurationFrom(d: DurationBuilder): FiniteDuration =
    Duration( d.millis, scala.concurrent.duration.MILLISECONDS)

  implicit def jodaDateTimeFrom(ts: Timestamp): DateTime = new DateTime(ts.getTime)

  implicit def timeStampFrom(localTime: DateTime): Timestamp = new Timestamp(localTime.getMillis)
}

object TimeConversion extends TimeConversion