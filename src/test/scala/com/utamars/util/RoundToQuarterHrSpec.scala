package com.utamars.util

import com.utamars.BaseSpec
import com.github.nscala_time.time.Imports._
import org.joda.time.Duration
import org.scalacheck.Gen
import scala.math._

class RoundToQuarterHrSpec extends BaseSpec with TimeImplicits {

  "DateTime#roundToQuarterHr" should {
    def dateTime(hr: Int, min: Int) = new DateTime(2016, 2, 4, hr, min)

    "round 8:07 to 8:00" in {
      dateTime(8, 7).roundToQuarterHr should equal(dateTime(8, 0))
    }

    "round 8:08 to 8:15" in {
      dateTime(8, 8).roundToQuarterHr should equal(dateTime(8, 15))
    }

    "round 8:22 to 8:15" in {
      dateTime(8, 22).roundToQuarterHr should equal(dateTime(8, 15))
    }

    "round 8:23 to 8:30" in {
      dateTime(8, 23).roundToQuarterHr should equal(dateTime(8, 30))
    }

    "round 8:37 to 8:30" in {
      dateTime(8, 37).roundToQuarterHr should equal(dateTime(8, 30))
    }

    "round 8:38 to 8:45" in {
      dateTime(8, 38).roundToQuarterHr should equal(dateTime(8, 45))
    }

    "round 8:52 to 8:45" in {
      dateTime(8, 52).roundToQuarterHr should equal(dateTime(8, 45))
    }

    "round 8:53 to 9:00" in {
      dateTime(8, 53).roundToQuarterHr should equal(dateTime(9, 0))
    }
  }

  "After rounding, the result" should {

    "be within 7 minute of the original time" in {
      val genDateTime = for {
        hr  <- Gen.choose(0, 23)
        min <- Gen.choose(0, 59)
      } yield new DateTime(2016, 2, 4, hr, min)

      forAll(genDateTime) { dt =>
        abs(new Duration(dt.roundToQuarterHr, dt).getStandardMinutes).toInt should be <= 7
      }
    }
  }

}
