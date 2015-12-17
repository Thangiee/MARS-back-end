package com.utamars.api

import spec.ServiceSpec
import com.github.nscala_time.time.Imports._

class TimeSheetGenServiceSpec extends ServiceSpec {

  "Time Sheet generation service" should {

  }

  "nextBusinessDays helper function" should {
    // https://www.uta.edu/business-affairs/payroll/payroll-processing-dates.php

    "be able to calculate the correct due date" in {
      new LocalDate(2015, 9, 15).nextBusinessDays(2) shouldEqual new LocalDate(2015, 9, 17)
      new LocalDate(2015, 9, 30).nextBusinessDays(2) shouldEqual new LocalDate(2015, 10, 2)

      new LocalDate(2015, 10, 15).nextBusinessDays(2) shouldEqual new LocalDate(2015, 10, 19)
      new LocalDate(2015, 10, 31).nextBusinessDays(2) shouldEqual new LocalDate(2015, 11, 3)

      new LocalDate(2015, 11, 15).nextBusinessDays(2) shouldEqual new LocalDate(2015, 11, 17)
      new LocalDate(2015, 11, 30).nextBusinessDays(2) shouldEqual new LocalDate(2015, 12, 2)

      new LocalDate(2015, 12, 15).nextBusinessDays(2) shouldEqual new LocalDate(2015, 12, 17)
      new LocalDate(2015, 12, 31).nextBusinessDays(2) shouldEqual new LocalDate(2016, 1, 5)

      new LocalDate(2016, 1, 15).nextBusinessDays(2) shouldEqual new LocalDate(2016, 1, 20)
      new LocalDate(2016, 1, 31).nextBusinessDays(2) shouldEqual new LocalDate(2016, 2, 2)

      new LocalDate(2016, 2, 15).nextBusinessDays(2) shouldEqual new LocalDate(2016, 2, 17)
      new LocalDate(2016, 2, 29).nextBusinessDays(2) shouldEqual new LocalDate(2016, 3, 2)

      new LocalDate(2016, 3, 15).nextBusinessDays(2) shouldEqual new LocalDate(2016, 3, 17)
      new LocalDate(2016, 3, 31).nextBusinessDays(2) shouldEqual new LocalDate(2016, 4, 4)

      new LocalDate(2016, 4, 15).nextBusinessDays(2) shouldEqual new LocalDate(2016, 4, 19)
      new LocalDate(2016, 4, 30).nextBusinessDays(2) shouldEqual new LocalDate(2016, 5, 3)

      new LocalDate(2016, 5, 15).nextBusinessDays(2) shouldEqual new LocalDate(2016, 5, 17)
      new LocalDate(2016, 5, 31).nextBusinessDays(2) shouldEqual new LocalDate(2016, 6, 2)

      new LocalDate(2016, 6, 15).nextBusinessDays(2) shouldEqual new LocalDate(2016, 6, 17)
      new LocalDate(2016, 6, 30).nextBusinessDays(2) shouldEqual new LocalDate(2016, 7, 5)

      new LocalDate(2016, 7, 15).nextBusinessDays(2) shouldEqual new LocalDate(2016, 7, 19)
      new LocalDate(2016, 7, 31).nextBusinessDays(2) shouldEqual new LocalDate(2016, 8, 2)

      new LocalDate(2016, 8, 15).nextBusinessDays(2) shouldEqual new LocalDate(2016, 8, 17)
      new LocalDate(2016, 8, 31).nextBusinessDays(2) shouldEqual new LocalDate(2016, 9, 2)
    }

    "be able to calculate the correct pay date" in {
      new LocalDate(2015, 9, 15).nextBusinessDays(5) shouldEqual new LocalDate(2015, 9, 22)
      new LocalDate(2015, 9, 30).nextBusinessDays(5) shouldEqual new LocalDate(2015, 10, 7)

      new LocalDate(2015, 10, 15).nextBusinessDays(5) shouldEqual new LocalDate(2015, 10, 22)
      new LocalDate(2015, 10, 31).nextBusinessDays(5) shouldEqual new LocalDate(2015, 11, 6)

      new LocalDate(2015, 11, 15).nextBusinessDays(5) shouldEqual new LocalDate(2015, 11, 20)
      new LocalDate(2015, 11, 30).nextBusinessDays(5) shouldEqual new LocalDate(2015, 12, 7)

      new LocalDate(2015, 12, 15).nextBusinessDays(5) shouldEqual new LocalDate(2015, 12, 22)
      new LocalDate(2015, 12, 31).nextBusinessDays(5) shouldEqual new LocalDate(2016, 1, 8)

      new LocalDate(2016, 1, 15).nextBusinessDays(5) shouldEqual new LocalDate(2016, 1, 25)
      new LocalDate(2016, 1, 31).nextBusinessDays(5) shouldEqual new LocalDate(2016, 2, 5)

      new LocalDate(2016, 2, 15).nextBusinessDays(5) shouldEqual new LocalDate(2016, 2, 22)
      new LocalDate(2016, 2, 29).nextBusinessDays(5) shouldEqual new LocalDate(2016, 3, 7)

      new LocalDate(2016, 3, 15).nextBusinessDays(5) shouldEqual new LocalDate(2016, 3, 23)
      new LocalDate(2016, 3, 31).nextBusinessDays(5) shouldEqual new LocalDate(2016, 4, 7)

      new LocalDate(2016, 4, 15).nextBusinessDays(5) shouldEqual new LocalDate(2016, 4, 22)
      new LocalDate(2016, 4, 30).nextBusinessDays(5) shouldEqual new LocalDate(2016, 5, 6)

      new LocalDate(2016, 5, 15).nextBusinessDays(5) shouldEqual new LocalDate(2016, 5, 20)
      new LocalDate(2016, 5, 31).nextBusinessDays(5) shouldEqual new LocalDate(2016, 6, 7)

      new LocalDate(2016, 6, 15).nextBusinessDays(5) shouldEqual new LocalDate(2016, 6, 22)
      new LocalDate(2016, 6, 30).nextBusinessDays(5) shouldEqual new LocalDate(2016, 7, 8)

      new LocalDate(2016, 7, 15).nextBusinessDays(5) shouldEqual new LocalDate(2016, 7, 22)
      new LocalDate(2016, 7, 31).nextBusinessDays(5) shouldEqual new LocalDate(2016, 8, 5)

      new LocalDate(2016, 8, 15).nextBusinessDays(5) shouldEqual new LocalDate(2016, 8, 22)
      new LocalDate(2016, 8, 31).nextBusinessDays(5) shouldEqual new LocalDate(2016, 9, 8)
    }
  }
}
