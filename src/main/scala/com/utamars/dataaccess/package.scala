package com.utamars

import java.sql.DriverManager
import java.util.UUID

import cats.data.Xor
import com.typesafe.config.ConfigFactory
import org.squeryl._
import org.squeryl.adapters.{PostgreSqlAdapter, H2Adapter}

import scala.util.{Failure, Success, Try}

package object dataaccess extends AnyRef with PrimitiveTypeMode {

  val config = ConfigFactory.load()
  val url = config.getString("db.url")
  val user = config.getString("db.user")
  val pass = config.getString("db.passwd")
  val driver = config.getString("db.driver")
  val adapter = driver match {
    case "org.postgresql.Driver" => new PostgreSqlAdapter
    case "org.h2.Driver"         => new H2Adapter
    case _                       =>
      throw new RuntimeException(s"$driver driver is not supported. Try using org.postgresql.Driver or org.h2.Driver.")
  }

  Class.forName(driver)
  SessionFactory.concreteFactory = Some(() => Session.create(DriverManager.getConnection(url, user, pass), adapter))

  type Role = String
  object Role {
    val Admin = "admin"
    val Instructor = "instructor"
    val Assistant = "assistant"
  }

  type Job = String
  object Job {
    val Grading  = "grading"
    val Teaching = "teaching"
  }

  sealed trait DataAccessErr
  case object NotFound extends DataAccessErr
  case class SqlErr(msg: String, detailedMsg: String) extends DataAccessErr
  case class InternalErr(err: Throwable) extends DataAccessErr

  def withErrHandling[A](a: => A): Xor[DataAccessErr, A] = Try(a) match {
    case Success(b)                       => Xor.Right(b)
    case Failure(ex: SquerylSQLException) => Xor.Left(SqlErr(Try(ex.getCause().getMessage).getOrElse(""), Try(ex.getMessage).getOrElse("")))
    case Failure(ex)                      => Xor.Left(InternalErr(ex))
  }

  def withErrHandlingOpt[A](a: => Option[A]): Xor[DataAccessErr, A] = Try(a) match {
    case Success(Some(b))                 => Xor.Right(b)
    case Success(None)                    => Xor.Left(NotFound)
    case Failure(ex: SquerylSQLException) => println(ex.getCause().getErrorCode); Xor.Left(SqlErr(ex.getCause().getMessage, ex.getMessage))
    case Failure(ex)                      => Xor.Left(InternalErr(ex))
  }

  implicit val accountKED = new KeyedEntityDef[Account, String]  {
    override def getId(a: Account): String = a.username
    override def idPropertyName: String = "username"
    override def isPersisted(a: Account): Boolean = true
  }

  implicit val assistantKED = new KeyedEntityDef[Assistant, String] {
    override def getId(a: Assistant): String = a.employeeId
    override def idPropertyName: String = "employeeId"
    override def isPersisted(a: Assistant): Boolean = true
  }

  implicit val instructorKED = new KeyedEntityDef[Instructor, UUID] {
    override def getId(a: Instructor): UUID = a.id
    override def idPropertyName: String = "id"
    override def isPersisted(a: Instructor): Boolean = true
  }

  implicit val clockInOutRecordKED = new KeyedEntityDef[ClockInOutRecord, UUID] {
    override def getId(a: ClockInOutRecord): UUID = a.id
    override def idPropertyName: String = "id"
    override def isPersisted(a: ClockInOutRecord): Boolean = true
  }
}
