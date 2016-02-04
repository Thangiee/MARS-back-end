package com.utamars.dataaccess

import java.io.FileOutputStream

import better.files._
import cats.data.XorT
import cats.std.all._
import com.github.nscala_time.time.Imports._
import com.itextpdf.text.pdf.{PdfReader, PdfStamper}
import com.utamars.util.UtaDate
import org.joda.time.{Days, LocalDate, ReadablePartial}

import scala.concurrent.Future

object TimeSheet {

  private val printDt = (pattern: String, date: ReadablePartial) =>  DateTimeFormat.forPattern(pattern).print(date)
  private val outDir = config.getString("service.timesheet.dir")

  def fromDateRange(range: (LocalDate, LocalDate), asst: Assistant): XorT[Future, DataAccessErr, File] = fromDateRange(range._1, range._2, asst)

  def fromDateRange(start: LocalDate, end: LocalDate, asst: Assistant): XorT[Future, DataAccessErr, File] = {

    def getTimeBox(date: LocalDate): String = {
      val days = Days.daysBetween(start, date).getDays
      val weeks = if (days > 7) 2 else 1
      s"$weeks[${days%8}]"
    }

    ClockInOutRecord.findBetween(start, end, asst.netId).map { records =>

      val reader = new PdfReader(getClass.getClassLoader.getResourceAsStream("timesheet-template.pdf"))
      if (!outDir.toFile.exists) outDir.toFile.createDirectories()
      val outFilepath = s"$outDir/timesheet_${asst.lastName}_${asst.firstName}_${printDt("MM-dd-YYYY", end)}.pdf"
      val stamper = new PdfStamper(reader, new FileOutputStream(outFilepath))
      val fields = stamper.getAcroFields

      // fill in assistant info
      fields.setField("Name", s"${asst.firstName} ${asst.lastName}")
      fields.setField("Id", asst.employeeId)
      fields.setField("Title", asst.title)
      fields.setField("Dept", asst.department)
      fields.setField("TitleCode", asst.titleCode)
      fields.setField("Period", printDt("MM-dd-YYYY", end))
      fields.setField("PayDate", printDt("MM-dd-YYYY", UtaDate.payDate(end)))
      fields.setField("DueDate", printDt("MM-dd-YYYY", UtaDate.dueDate(end)))
      fields.setField("PercentOfTime", "<50%")
      fields.setField("Rate", "$" + s"${asst.rate}/hr")

      // fill in the Day and Date rows
      (0 to Days.daysBetween(start, end).getDays).foreach { i =>
        val current = start + i.days
        val timeBox = getTimeBox(current)
        fields.setField(s"Day$timeBox", printDt("EE", current))
        fields.setField(s"Date$timeBox", printDt("MM/dd", current))
      }

      val completedRecords = records.filter(_.outTime.isDefined) // i.e. records that has been clocked out
      val recordsGroupBySameDay = completedRecords.groupBy(r => new DateTime(r.inTime.getMillis).dayOfMonth()).values

      // fill in clock in time cells
      recordsGroupBySameDay.foreach { sameDayRecords =>
        val (amRecords, pmRecords) = sameDayRecords.reverse.partition(_.inTime.getHourOfDay <= 12)

        if (amRecords.nonEmpty) {
          val inTimes = amRecords.map(r => printDt("h:mm", r.inTime.toLocalTime)).mkString("\n")
          fields.setField("AmIn" + getTimeBox(amRecords.head.inTime), inTimes)
        }

        if (pmRecords.nonEmpty) {
          val inTimes = pmRecords.map(r => printDt("h:mm", r.inTime.toLocalTime)).mkString("\n")
          fields.setField("PmIn" + getTimeBox(pmRecords.head.inTime), inTimes)
        }
      }

      // fill in clock out time cells
      recordsGroupBySameDay.foreach { sameDayRecords =>
        val (amRecords, pmRecords) = sameDayRecords.reverse.partition(_.outTime.get.getHourOfDay <= 12)

        if (amRecords.nonEmpty) {
          val outTimes = amRecords.map(r => printDt("h:mm", r.outTime.get.toLocalTime)).mkString("\n")
          fields.setField("AmOut" + getTimeBox(amRecords.head.outTime.get), outTimes)
        }

        if (pmRecords.nonEmpty) {
          val outTimes = pmRecords.map(r => printDt("h:mm", r.outTime.get.toLocalTime)).mkString("\n")
          fields.setField("PmOut" + getTimeBox(pmRecords.head.outTime.get), outTimes)
        }
      }

      val payPeriodTotalTime = recordsGroupBySameDay.map { sameDayRecords =>
        val total = sameDayRecords.foldRight(new Duration(0))((r,total) => new Duration(r.inTime.getTime, r.outTime.get.getTime) + total)
        // file in the daily total hrs
        fields.setExtraMargin(0, 8) // top margins to center vertically
        fields.setField(s"Total${getTimeBox(sameDayRecords.head.outTime.get)}", f"${total.getStandardHours + total.getStandardMinutes % 60 / 60.0}%1.2f")
        fields.setExtraMargin(0, 0) // reset margins

        total // return the the daily total so we can sum up the whole pay period total
      }.foldLeft(new Duration(0))(_ + _)

      // fill in the pay period total hours
      fields.setField("TotalHours", f"${payPeriodTotalTime.getStandardHours + payPeriodTotalTime.getStandardMinutes % 60 / 60.0}%1.2f")

      stamper.close()
      reader.close()

      outFilepath.toFile
    }
  }
}