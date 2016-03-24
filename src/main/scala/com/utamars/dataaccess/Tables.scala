package com.utamars.dataaccess

import java.sql.Timestamp

import scala.language.postfixOps

private [dataaccess] trait Tables {
  val driver: slick.driver.JdbcProfile
  import driver.api._

  private[dataaccess] lazy val AssistantTable        = new TableQuery(tag => new AssistantTable(tag))
  private[dataaccess] lazy val AccountTable          = new TableQuery(tag => new AccountTable(tag))
  private[dataaccess] lazy val ClockInOutRecordTable = new TableQuery(tag => new ClockInOutRecordTable(tag))
  private[dataaccess] lazy val InstructorTable       = new TableQuery(tag => new InstructorTable(tag))
  private[dataaccess] lazy val FaceImageTable        = new TableQuery(tag => new FaceImageTable(tag))

  protected lazy val schema = AssistantTable.schema ++ AccountTable.schema ++ ClockInOutRecordTable.schema ++
    InstructorTable.schema ++ FaceImageTable.schema

  class AccountTable(_tableTag: Tag) extends Table[Account](_tableTag, "account") {
    def * = (netId, username, passwd, role, createTime, approve) <>(Account.apply _ tupled, Account.unapply)

    val netId     : Rep[String]    = column[String]("net_id", O.PrimaryKey, O.Length(128, varying = true))
    val username  : Rep[String]    = column[String]("username", O.Length(128, varying = true))
    val passwd    : Rep[String]    = column[String]("passwd", O.Length(128, varying = true))
    val role      : Rep[String]    = column[String]("role", O.Length(128, varying = true))
    val createTime: Rep[Timestamp] = column[java.sql.Timestamp]("create_time")
    val approve   : Rep[Boolean]   = column[Boolean]("approve", O.Default(false))

    val index1 = index("account_idx", username, unique = true)
  }

  class AssistantTable(_tableTag: Tag) extends Table[Assistant](_tableTag, "assistant") {
    def * = (netId, rate, email, job, department, lastName, firstName, employeeId, title, titleCode, threshold) <> (Assistant.apply _ tupled, Assistant.unapply)

    val netId     : Rep[String] = column[String]("net_id", O.PrimaryKey, O.Length(128, varying = true))
    val rate      : Rep[Double] = column[Double]("rate")
    val email     : Rep[String] = column[String]("email", O.Length(128, varying = true))
    val job       : Rep[String] = column[String]("job", O.Length(128, varying = true))
    val department: Rep[String] = column[String]("department", O.Length(128, varying = true))
    val lastName  : Rep[String] = column[String]("last_name", O.Length(128, varying = true))
    val firstName : Rep[String] = column[String]("first_name", O.Length(128, varying = true))
    val employeeId: Rep[String] = column[String]("employee_id", O.Length(128, varying = true))
    val title     : Rep[String] = column[String]("title", O.Length(128, varying = true))
    val titleCode : Rep[String] = column[String]("title_code", O.Length(128, varying = true))
    val threshold : Rep[Double] = column[Double]("threshold")

    lazy val accountTableFk = foreignKey("assistant_net_id_fkey", netId, AccountTable)(r => r.netId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)

    val index1 = index("assistant_idx", email, unique=true)
  }

  class ClockInOutRecordTable(_tableTag: Tag) extends Table[ClockInOutRecord](_tableTag, "clock_in_out_record") {
    def * = (id, netId, inTime, outTime, inComputerId, outComputerId) <>(ClockInOutRecord.apply _ tupled, ClockInOutRecord.unapply)

    val id           : Rep[Option[Int]]       = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
    val netId        : Rep[String]            = column[String]("net_id", O.Length(128, varying = true))
    val inTime       : Rep[Timestamp]         = column[Timestamp]("in_time")
    val outTime      : Rep[Option[Timestamp]] = column[Option[Timestamp]]("out_time", O.Default(None))
    val inComputerId : Rep[Option[String]]    = column[Option[String]]("in_computer_id", O.Length(128, varying = true))
    val outComputerId: Rep[Option[String]]    = column[Option[String]]("out_computer_id", O.Length(128, varying = true))

    lazy val assistantTableFk = foreignKey("clock_in_out_record_net_id_fkey", netId, AssistantTable)(r => r.netId, onUpdate=ForeignKeyAction.NoAction)
  }

  class InstructorTable(_tableTag: Tag) extends Table[Instructor](_tableTag, "instructor") {
    def * = (netId, email, lastName, firstName) <> (Instructor.apply _ tupled, Instructor.unapply)

    val netId    : Rep[String] = column[String]("net_id", O.PrimaryKey, O.Length(128, varying = true))
    val email    : Rep[String] = column[String]("email", O.Length(128, varying = true))
    val lastName : Rep[String] = column[String]("last_name", O.Length(128, varying = true))
    val firstName: Rep[String] = column[String]("first_name", O.Length(128, varying = true))

    lazy val accountTableFk = foreignKey("instructor_net_id_fkey", netId, AccountTable)(r => r.netId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)

    val index1 = index("instructor_idx", email, unique=true)
  }

  class FaceImageTable(_tableTag: Tag) extends Table[FaceImage](_tableTag, "face_image") {
    def * = (id, netId, faceId) <> (FaceImage.apply _ tupled, FaceImage.unapply)

    val id    : Rep[String] = column[String]("id", O.PrimaryKey, O.Length(128, varying = true))
    val netId : Rep[String] = column[String]("net_id", O.Length(128, varying = true))
    val faceId: Rep[String] = column[String]("face_id", O.Length(128, varying = true))

    lazy val assistantTableFk = foreignKey("face_image_net_id_fkey", netId, AssistantTable)(asst => asst.netId, onUpdate=ForeignKeyAction.NoAction)
  }
}
