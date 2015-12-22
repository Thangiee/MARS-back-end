package com.utamars.dataaccess

import cats.data.Xor
import com.utamars.BaseSpec
import org.joda.time.LocalDate
import scala.concurrent.duration._
import better.files._

import scala.concurrent.Await

class TimeSheetSpec extends BaseSpec {

  val outDir = config.getString("service.timesheet.dir")

  override def beforeAll(): Unit = {
    DB.createSchema()
    initDataBaseData()
  }

  override def afterAll(): Unit = {
    outDir.toFile.children.foreach(f => f.delete())
  }

  "Generated time sheet" should {

    "be placed in the directory specified by service.timesheet.dir in application.config" in {
      val genTimeSheet = TimeSheet.fromDateRange(new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 15), asstBob)

      Await.result(genTimeSheet.value, 1.minute) match {
        case Xor.Right(timeSheet) => timeSheet.path.parent.path shouldEqual outDir.toFile.path
        case Xor.Left(err) => fail(s"Expected Xor.Right but got $err")
      }
    }
  }

}
