package com.utamars.util

import com.utamars.BaseSpec
import com.utamars.util.UtaDate._
import org.joda.time.LocalDate

class UtaDateSpec extends BaseSpec {

  "UtaDate" should {
    // https://www.uta.edu/business-affairs/payroll/payroll-processing-dates.php

    "calculate the correct due date" in {
      dueDate(new LocalDate(2015, 9, 15)) shouldEqual new LocalDate(2015, 9, 17)
      dueDate(new LocalDate(2015, 9, 30)) shouldEqual new LocalDate(2015, 10, 2)

      dueDate(new LocalDate(2015, 10, 15)) shouldEqual new LocalDate(2015, 10, 19)
      dueDate(new LocalDate(2015, 10, 31)) shouldEqual new LocalDate(2015, 11, 3)

      dueDate(new LocalDate(2015, 11, 15)) shouldEqual new LocalDate(2015, 11, 17)
      dueDate(new LocalDate(2015, 11, 30)) shouldEqual new LocalDate(2015, 12, 2)

      dueDate(new LocalDate(2015, 12, 15)) shouldEqual new LocalDate(2015, 12, 17)
      dueDate(new LocalDate(2015, 12, 31)) shouldEqual new LocalDate(2016, 1, 5)

      dueDate(new LocalDate(2016, 1, 15)) shouldEqual new LocalDate(2016, 1, 20)
      dueDate(new LocalDate(2016, 1, 31)) shouldEqual new LocalDate(2016, 2, 2)

      dueDate(new LocalDate(2016, 2, 15)) shouldEqual new LocalDate(2016, 2, 17)
      dueDate(new LocalDate(2016, 2, 29)) shouldEqual new LocalDate(2016, 3, 2)

      dueDate(new LocalDate(2016, 3, 15)) shouldEqual new LocalDate(2016, 3, 17)
      dueDate(new LocalDate(2016, 3, 31)) shouldEqual new LocalDate(2016, 4, 4)

      dueDate(new LocalDate(2016, 4, 15)) shouldEqual new LocalDate(2016, 4, 19)
      dueDate(new LocalDate(2016, 4, 30)) shouldEqual new LocalDate(2016, 5, 3)

      dueDate(new LocalDate(2016, 5, 15)) shouldEqual new LocalDate(2016, 5, 17)
      dueDate(new LocalDate(2016, 5, 31)) shouldEqual new LocalDate(2016, 6, 2)

      dueDate(new LocalDate(2016, 6, 15)) shouldEqual new LocalDate(2016, 6, 17)
      dueDate(new LocalDate(2016, 6, 30)) shouldEqual new LocalDate(2016, 7, 5)

      dueDate(new LocalDate(2016, 7, 15)) shouldEqual new LocalDate(2016, 7, 19)
      dueDate(new LocalDate(2016, 7, 31)) shouldEqual new LocalDate(2016, 8, 2)

      dueDate(new LocalDate(2016, 8, 15)) shouldEqual new LocalDate(2016, 8, 17)
      dueDate(new LocalDate(2016, 8, 31)) shouldEqual new LocalDate(2016, 9, 2)
    }

    "calculate the correct pay date" in {
      payDate(new LocalDate(2015, 9, 15)) shouldEqual new LocalDate(2015, 9, 22)
      payDate(new LocalDate(2015, 9, 30)) shouldEqual new LocalDate(2015, 10, 7)

      payDate(new LocalDate(2015, 10, 15)) shouldEqual new LocalDate(2015, 10, 22)
      payDate(new LocalDate(2015, 10, 31)) shouldEqual new LocalDate(2015, 11, 6)

      payDate(new LocalDate(2015, 11, 15)) shouldEqual new LocalDate(2015, 11, 20)
      payDate(new LocalDate(2015, 11, 30)) shouldEqual new LocalDate(2015, 12, 7)

      payDate(new LocalDate(2015, 12, 15)) shouldEqual new LocalDate(2015, 12, 22)
      payDate(new LocalDate(2015, 12, 31)) shouldEqual new LocalDate(2016, 1, 8)

      payDate(new LocalDate(2016, 1, 15)) shouldEqual new LocalDate(2016, 1, 25)
      payDate(new LocalDate(2016, 1, 31)) shouldEqual new LocalDate(2016, 2, 5)

      payDate(new LocalDate(2016, 2, 15)) shouldEqual new LocalDate(2016, 2, 22)
      payDate(new LocalDate(2016, 2, 29)) shouldEqual new LocalDate(2016, 3, 7)

      payDate(new LocalDate(2016, 3, 15)) shouldEqual new LocalDate(2016, 3, 23)
      payDate(new LocalDate(2016, 3, 31)) shouldEqual new LocalDate(2016, 4, 7)

      payDate(new LocalDate(2016, 4, 15)) shouldEqual new LocalDate(2016, 4, 22)
      payDate(new LocalDate(2016, 4, 30)) shouldEqual new LocalDate(2016, 5, 6)

      payDate(new LocalDate(2016, 5, 15)) shouldEqual new LocalDate(2016, 5, 20)
      payDate(new LocalDate(2016, 5, 31)) shouldEqual new LocalDate(2016, 6, 7)

      payDate(new LocalDate(2016, 6, 15)) shouldEqual new LocalDate(2016, 6, 22)
      payDate(new LocalDate(2016, 6, 30)) shouldEqual new LocalDate(2016, 7, 8)

      payDate(new LocalDate(2016, 7, 15)) shouldEqual new LocalDate(2016, 7, 22)
      payDate(new LocalDate(2016, 7, 31)) shouldEqual new LocalDate(2016, 8, 5)

      payDate(new LocalDate(2016, 8, 15)) shouldEqual new LocalDate(2016, 8, 22)
      payDate(new LocalDate(2016, 8, 31)) shouldEqual new LocalDate(2016, 9, 8)
    }
  }
}
