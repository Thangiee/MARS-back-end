package com.utamars.dataaccess

import akka.dispatch.ExecutionContexts
import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import org.h2.jdbc.JdbcSQLException
import slick.dbio.{DBIOAction, NoStream}

import scala.concurrent.forkjoin.ForkJoinPool
import scala.concurrent.{Await, Future}
import scala.util.Try

object DB extends AnyRef with Tables {
  private val config = ConfigFactory.load()
  implicit val executionCtx = ExecutionContexts.fromExecutor(new ForkJoinPool(config.getInt("db.parallelism")))

  val driver = config.getString("db.driver") match {
    case "org.postgresql.Driver" => slick.driver.PostgresDriver
    case "org.h2.Driver"         => slick.driver.H2Driver
    case _                       => throw new RuntimeException(s"Driver is not supported. Try using org.postgresql.Driver or org.h2.Driver.")
  }
  private val db = driver.api.Database.forConfig("db")

  def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = db.run(a)

  def createSchema(): Unit = {
    import driver.api._
    Try(Await.result(db.run(schema.create), 1.minute)).recover {
      case e: JdbcSQLException => println(e.getMessage)
      case e => e.printStackTrace()
    }
  }

  def dropSchema(): Unit = {
    import driver.api._
    Await.result(db.run(schema.drop), 1.minute)
  }
}
