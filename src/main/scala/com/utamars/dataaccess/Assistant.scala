package com.utamars.dataaccess

import java.sql.Timestamp

import cats.data.{Xor, XorT}
import cats.implicits._
import com.utamars.dataaccess.DB.driver.api._
import com.utamars.forms.UpdateAssistantForm
import slick.jdbc.GetResult

import scala.concurrent.Future

case class Assistant(netId: String, rate: Double, email: String, job: String, department: String,
  lastName: String, firstName: String, employeeId: String, title: String, titleCode: String, threshold: Double)

case class ClockInAsst(netId: String, imgId: String, fName: String, lName: String, inTime: Timestamp)
object ClockInAsst {
  implicit val getResult = GetResult(r => ClockInAsst(r.<<, r.<<, r.<<, r.<<, r.<<))
}

object Assistant {
  private val asstAccTable = for { (asst, acc) <- DB.AssistantTable join DB.AccountTable on (_.netId === _.netId) } yield (asst, acc)

  def all(): XorT[Future, DataAccessErr, Seq[Assistant]] = DB.AssistantTable.result

  def allWithAcc(): XorT[Future, DataAccessErr, Seq[(Assistant, Account)]] = asstAccTable.result

  def findByNetId(netId: String): XorT[Future, DataAccessErr, Assistant] =
    DB.AssistantTable.filter(_.netId.toLowerCase === netId.toLowerCase).result.headOption

  def findByNetIdWithAcc(netId: String): XorT[Future, DataAccessErr, (Assistant, Account)] =
    asstAccTable.filter(_._1.netId === netId).result.headOption

  def findByNetIds(netIds: Set[String]): XorT[Future, DataAccessErr, Seq[Assistant]] =
    DB.AssistantTable.filter(_.netId inSetBind netIds).result

  def findByNetIdsWithAcc(netIds: Set[String]): XorT[Future, DataAccessErr, Seq[(Assistant, Account)]] =
    asstAccTable.filter(_._1.netId inSetBind netIds).result

  def deleteAll(): XorT[Future, DataAccessErr, Unit] = DB.AssistantTable.filter(a => a.netId === a.netId).delete

  def update(netId: String, form: UpdateAssistantForm): XorT[Future, DataAccessErr, Unit] = {
    findByNetId(netId).flatMap { asst =>
      asst.copy(
        rate = form.rate.getOrElse(asst.rate),
        department = form.department.getOrElse(asst.department),
        title = form.title.getOrElse(asst.title),
        titleCode = form.titleCode.getOrElse(asst.titleCode),
        threshold = form.threshold.getOrElse(asst.threshold)
      ).update()
    }
  }

  def test: XorT[Future, DataAccessErr, Vector[ClockInAsst]] = XorT(
    DB.run(
    sql"""SELECT DISTINCT ON (a.net_id) a.net_id, f.id, a.first_name, a.last_name, r.in_time
          FROM assistant a
          LEFT JOIN face_image f USING (net_id)
          LEFT JOIN clock_in_out_record r USING (net_id) WHERE r.out_time IS NULL
          ORDER BY a.net_id, f.id, r.in_time DESC""".as[ClockInAsst]
    ).map(res => Xor.Right(res)).recover(defaultErrHandler)
  )

  implicit class PostfixOps(asst: Assistant) {
    def create(): XorT[Future, DataAccessErr, Unit] = DB.AssistantTable += asst

    def update(): XorT[Future, DataAccessErr, Unit] = DB.AssistantTable.filter(_.netId === asst.netId).update(asst)
  }
}
