package com.utamars.dataaccess

import cats.data.XorT
import com.utamars.dataaccess.DB.driver.api._

import scala.concurrent.Future

case class Instructor(netId: String, email: String, lastName: String, firstName: String)

object Instructor {

  def findBy(netId: String): XorT[Future, DataAccessErr, Instructor] =
    withErrHandlingOpt(DB.InstructorTable.filter(_.netId.toLowerCase === netId.toLowerCase).result.headOption)

  def deleteAll(): XorT[Future, DataAccessErr, Unit] = {
    withErrHandling(DBIO.seq(DB.InstructorTable.filter(i => i.netId === i.netId).delete))
  }

  implicit class PostfixOps(instructor: Instructor) {
    def create(): XorT[Future, DataAccessErr, Unit] = withErrHandling(DBIO.seq(DB.InstructorTable += instructor))
  }
}
