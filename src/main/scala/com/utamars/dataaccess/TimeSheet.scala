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
  private case class Record(inTime: DateTime, outTime: DateTime)

  def fromDateRange(range: (LocalDate, LocalDate), asst: Assistant): XorT[Future, DataAccessErr, File] = fromDateRange(range._1, range._2, asst)

  def fromDateRange(start: LocalDate, end: LocalDate, asst: Assistant): XorT[Future, DataAccessErr, File] = {

    // compute the id of a in/out time field in timesheet-template.pdf
    def getTimeBox(date: LocalDate): String = {
      val days = Days.daysBetween(start, date).getDays
      val weeks = if (days > 7) 2 else 1
      s"$weeks[${days%8}]"
    }

    ClockInOutRecord.findBetween(start, end, asst.netId, inclusive=false).map { records => // get records that has been clocked out

      val reader = new PdfReader(getClass.getClassLoader.getResourceAsStream("timesheet-template.pdf"))
      if (!outDir.toFile.exists) outDir.toFile.createDirectories()
      val outFilepath = s"$outDir/timesheet_${asst.lastName}_${asst.firstName}_${printDt("MM-dd-YYYY", end)}.pdf"
      val stamper = new PdfStamper(reader, new FileOutputStream(outFilepath))
      val fields = stamper.getAcroFields

      // fill in assistant info
      fields.setField("Name", s"${asst.firstName.capitalize} ${asst.lastName.capitalize}")
      fields.setField("Id", asst.employeeId)
      fields.setField("Title", asst.title.capitalize)
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

      val recordsGroupBySameDay = records
        .map(r => Record(timestamp2DateTime(r.inTime).roundToQuarterHr, timestamp2DateTime(r.outTime.get).roundToQuarterHr))
        .groupBy(_.inTime.dayOfMonth())
        .values

      // fill in clock in time cells
      val inTimesGroupBySameDay  = recordsGroupBySameDay.map(_.map(_.inTime))
      inTimesGroupBySameDay.foreach { sameDayTimes =>
        val (amTimes, pmTimes) = sameDayTimes.reverse.partition(_.getHourOfDay <= 12)

        if (amTimes.nonEmpty) {
          val inTimes = amTimes.map(t => printDt("h:mm", t.toLocalTime)).mkString("\n")
          fields.setField("AmIn" + getTimeBox(amTimes.head.toLocalDate), inTimes)
        }

        if (pmTimes.nonEmpty) {
          val inTimes = pmTimes.map(t => printDt("h:mm", t.toLocalTime)).mkString("\n")
          fields.setField("PmIn" + getTimeBox(pmTimes.head.toLocalDate), inTimes)
        }
      }

      // fill in clock out time cells
      val outTimesGroupBySameDay = recordsGroupBySameDay.map(_.map(_.outTime))
      outTimesGroupBySameDay.foreach { sameDayTimes =>
        val (amTimes, pmTimes) = sameDayTimes.reverse.partition(_.getHourOfDay <= 12)

        if (amTimes.nonEmpty) {
          val outTimes = amTimes.map(t => printDt("h:mm", t.toLocalTime)).mkString("\n")
          fields.setField("AmOut" + getTimeBox(amTimes.head.toLocalDate), outTimes)
        }

        if (pmTimes.nonEmpty) {
          val outTimes = pmTimes.map(t => printDt("h:mm", t.toLocalTime)).mkString("\n")
          fields.setField("PmOut" + getTimeBox(pmTimes.head.toLocalDate), outTimes)
        }
      }

      val payPeriodTotalTime = recordsGroupBySameDay.map { sameDayRecords =>
        val total = sameDayRecords.foldRight(new Duration(0))((r, total) => new Duration(r.inTime, r.outTime) + total)
        // file in the daily total hrs
        fields.setExtraMargin(0, 8) // top margins to center vertically
        fields.setField(s"Total${getTimeBox(sameDayRecords.head.outTime.toLocalDate)}", f"${total.getStandardSeconds / 3600.0}%1.2f")
        fields.setExtraMargin(0, 0) // reset margins

        total // return the the daily total so we can sum up the whole pay period total
      }.foldLeft(new Duration(0))(_ + _)

      // fill in the pay period total hours
      fields.setField("TotalHours", f"${payPeriodTotalTime.getStandardSeconds / 3600.0}%1.2f")

      stamper.close()
      reader.close()

      outFilepath.toFile
    }
  }
}