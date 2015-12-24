package com.utamars

import cats.data.{Xor, XorT}
import com.utamars.util.TimeConversion
import org.postgresql.util.PSQLException
import slick.dbio.{DBIOAction, NoStream}

import scala.concurrent.Future
import scala.language.implicitConversions

package object dataaccess extends AnyRef with TimeConversion {

  private[dataaccess] implicit val ec = DB.executionCtx

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
  case class SqlDuplicateKey(msg: String) extends DataAccessErr
  case class SqlErr(code: String, msg: String) extends DataAccessErr
  case class InternalErr(err: Throwable) extends DataAccessErr

  def withErrHandling[A](a: => Future[A]) = XorT[Future, DataAccessErr, A] {
    a.map(b => Xor.Right(b)).recover(defaultErrHandler)
  }

  def withErrHandlingOpt[A](a: => Future[Option[A]]) = XorT[Future, DataAccessErr, A] {
    a.map(b => if (b.isDefined) Xor.Right(b.get) else Xor.Left(NotFound)).recover(defaultErrHandler)
  }
  private val defaultErrHandler = PartialFunction[Throwable, Xor.Left[DataAccessErr]] {
    case ex: PSQLException =>
      ex.getSQLState match {
        case "23505" => Xor.Left(SqlDuplicateKey(ex.getServerErrorMessage.getDetail))
        case code    => Xor.Left(SqlErr(code, ex.getServerErrorMessage.getDetail))
      }
    case ex          => Xor.Left(InternalErr(ex))
  }

  private[dataaccess] implicit def executeFromDb[A](action: DBIOAction[A, NoStream, Nothing]): Future[A] = DB.run(action)
}
