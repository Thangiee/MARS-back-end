package com.utamars

import java.sql.BatchUpdateException

import cats.data.{Xor, XorT}
import com.utamars.dataaccess.tables._
import com.utamars.util.TimeConversion
import slick.SlickException
import slick.dbio.{DBIOAction, NoStream}

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.Try

package object dataaccess extends AnyRef with TimeConversion {

  private implicit val ec = DB.executionCtx

  type Role = String
  object Role {
    val Admin = "admin"
    val Instructor = "instructor"
    val Assistant = "assistant"
  }

  type Job = String
  object Job {
    val Grading = "grading"
    val Teaching = "teaching"
  }

  sealed trait DataAccessErr
  case object NotFound extends DataAccessErr
  case class SqlErr(msg: String, cause: Throwable) extends DataAccessErr
  case class InternalErr(err: Throwable) extends DataAccessErr

  def withErrHandling[A](a: => Future[A]) = XorT[Future, DataAccessErr, A] {
    a.map(b => Xor.Right(b)).recover(defaultErrHandler)
  }

  def withErrHandlingOpt[A](a: => Future[Option[A]]) = XorT[Future, DataAccessErr, A] {
    a.map(b => if (b.isDefined) Xor.Right(b.get) else Xor.Left(NotFound)).recover(defaultErrHandler)
  }
  private val defaultErrHandler = PartialFunction[Throwable, Xor.Left[DataAccessErr]] {
    case ex: SlickException       => Xor.Left(SqlErr(Try(ex.getMessage).getOrElse("Err msg is null!"), ex.getCause))
    case ex: BatchUpdateException => Xor.Left(SqlErr(ex.getNextException.getMessage, ex))
    case ex                       => Xor.Left(InternalErr(ex))
  }

  private[dataaccess] implicit def executeFromDb[A](action: DBIOAction[A, NoStream, Nothing]): Future[A] = DB.run(action)
}
