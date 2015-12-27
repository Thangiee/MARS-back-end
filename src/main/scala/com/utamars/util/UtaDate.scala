package com.utamars.util

import java.net.URL

import de.jollyday.{ManagerParameters, HolidayManager}
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory
import org.joda.time.{DateTimeConstants, LocalDate}
import scala.collection.JavaConversions._

trait UtaDate {

  /** Calculate the due date from the given date.
    * The due date is defined by UTA to be 2 business days after the
    * end date of a pay period. */
  def dueDate(endDate: LocalDate): LocalDate = nextBusinessDays(endDate, days=2)

  /** Calculate the pay date from the given date.
    * The pay date is defined by UTA to be 5 business days after the
    * end date of a pay period. */
  def payDate(endDate: LocalDate): LocalDate = nextBusinessDays(endDate, days=5)

  private def nextBusinessDays(localDate: LocalDate, days: Int): LocalDate = {
    val m = HolidayManager.getInstance(ManagerParameters.create(getClass.getClassLoader.getResource("uta-holidays.xml")))
    val year = localDate.year().get()
    val holidays = m.getHolidays(year).map(_.getDate) ++ m.getHolidays(year+1).map(_.getDate)
    val holidayCalendar = new DefaultHolidayCalendar[LocalDate](holidays)
    val cal = LocalDateKitCalculatorsFactory.forwardCalculator("uta")
    cal.setStartDate(localDate)
    cal.setHolidayCalendar(holidayCalendar)

    if (localDate.getDayOfWeek == DateTimeConstants.SATURDAY || localDate.getDayOfWeek == DateTimeConstants.SUNDAY)
      cal.moveByBusinessDays(days-1).getCurrentBusinessDate
    else
      cal.moveByBusinessDays(days).getCurrentBusinessDate
  }
}

object UtaDate extends UtaDate
