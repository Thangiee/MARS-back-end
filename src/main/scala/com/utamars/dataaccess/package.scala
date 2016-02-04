package com.utamars

import akka.dispatch.ExecutionContexts
import cats.data.{Xor, XorT}
import com.typesafe.config.ConfigFactory
import com.utamars.util.TimeImplicits
import org.postgresql.util.PSQLException
import slick.dbio.{Effect, DBIOAction, NoStream}

import scala.concurrent.Future
import scala.concurrent.forkjoin.ForkJoinPool
import scala.language.implicitConversions

package object dataaccess extends AnyRef with TimeImplicits {

  private[dataaccess] val config = ConfigFactory.load()
  private val parallelism: Int = config.getInt("db.parallelism")
  private[dataaccess] implicit val executionCtx = ExecutionContexts.fromExecutor(new ForkJoinPool(parallelism))

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

  private val defaultErrHandler = PartialFunction[Throwable, Xor.Left[DataAccessErr]] {
    case ex: PSQLException =>
      ex.getSQLState match {
        case "23505" => Xor.Left(SqlDuplicateKey(ex.getServerErrorMessage.getDetail))
        case code    => Xor.Left(SqlErr(code, ex.getServerErrorMessage.getDetail))
      }
    case ex          => Xor.Left(InternalErr(ex))
  }

  private[dataaccess] implicit def withErrHandling[A, B <: NoStream, C <: Effect](action: DBIOAction[A,B,C]): XorT[Future, DataAccessErr, A] =
    XorT(DB.run(action).map(a => Xor.Right(a)).recover(defaultErrHandler))

  private[dataaccess] implicit def withErrHandling_[B <: NoStream, C <: Effect](action: DBIOAction[_,B,C]): XorT[Future, DataAccessErr, Unit] =
    XorT(DB.run(action).map(_ => Xor.Right()).recover(defaultErrHandler))

  private[dataaccess] implicit def withErrHandlingUnit[B <: NoStream, C <: Effect](action: DBIOAction[Unit,B,C]): XorT[Future, DataAccessErr, Unit] =
    XorT(DB.run(action).map(_ => Xor.Right()).recover(defaultErrHandler))

  private[dataaccess] implicit def withErrHandlingOpt[A, B <: NoStream, C <: Effect](action: DBIOAction[Option[A],B,C]): XorT[Future, DataAccessErr, A] =
    XorT(DB.run(action).map(a => if (a.isDefined) Xor.Right(a.get) else Xor.Left(NotFound)).recover(defaultErrHandler))
}
