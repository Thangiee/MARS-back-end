package com.utamars.dataaccess

import java.io.FileOutputStream

import better.files._
import cats.data.XorT
import cats.std.all._
import com.github.nscala_time.time.Imports._
import com.itextpdf.text.pdf.{PdfReader, PdfStamper}
import com.typesafe.config.ConfigFactory
import com.utamars.util.UtaDate
import org.joda.time.{Days, LocalDate, ReadablePartial}

import scala.concurrent.Future

// todo: what to do on with multiple clock in/out on the same day
// todo: how to fill time cell when clocks out after midnight
// todo: what to do when assistant has not clock out before the end of a pay period
object TimeSheet {

  private val printDt = (pattern: String, date: ReadablePartial) =>  DateTimeFormat.forPattern(pattern).print(date)
  private val config = ConfigFactory.load()
  private val outDir = config.getString("service.timesheet.dir")

  def fromDateRange(range: (LocalDate, LocalDate), asst: Assistant): XorT[Future, DataAccessErr, File] = fromDateRange(range._1, range._2, asst)

  def fromDateRange(start: LocalDate, end: LocalDate, asst: Assistant): XorT[Future, DataAccessErr, File] = {

    def getTimeBox(date: LocalDate): String = {
      val days = Days.daysBetween(start, date).getDays
      val weeks = if (days > 7) 2 else 1
      s"$weeks[${days%8}]"
    }

    ClockInOutRecord.findBetween(start, end, asst.netId).map { records =>

      val reader = new PdfReader("timesheet-template.pdf")
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
      fields.setField("PercentOfTime", "TODO") // todo: input type?
      fields.setField("Rate", "$" + s"${asst.rate}/hr")

      // fill in the Day and Date rows
      (0 to Days.daysBetween(start, end).getDays).foreach { i =>
        val current = start + i.days
        val timeBox = getTimeBox(current)
        fields.setField(s"Day$timeBox", printDt("EE", current))
        fields.setField(s"Date$timeBox", printDt("MM/dd", current))
      }

      val payPeriodTotalTime = records.map { r =>
        // fill in clock in time cells
        val inTimeBoxField = (if (r.inTime.getHourOfDay <= 12) "AmIn" else "PmIn") + getTimeBox(r.inTime)
        fields.setField(inTimeBoxField, printDt("h:mm", r.inTime.toLocalTime))

        // fill in clock out time cells
        val outTime = r.outTime.getOrElse(end.toEndOfDayTs)
        val outTimeBoxField = (if (outTime.getHourOfDay <= 12) "AmOut" else "PmOut") + getTimeBox(outTime)
        fields.setField(outTimeBoxField, printDt("h:mm", outTime.toLocalTime))

        // file in the daily total hrs
        val total = new Duration(r.inTime.getTime, outTime.getTime)
        fields.setExtraMargin(0, 8) // top margins to center vertically
        fields.setField(s"Total${getTimeBox(r.inTime)}", f"${total.getStandardHours + total.getStandardMinutes % 60 / 60.0}%1.2f")
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