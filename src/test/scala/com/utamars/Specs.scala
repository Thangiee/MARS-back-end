package com.utamars

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.softwaremill.session.{InMemoryRefreshTokenStorage, SessionConfig, SessionManager, SessionUtil}
import com.typesafe.config.ConfigFactory
import com.utamars.api.Username
import com.utamars.dataaccess._
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}
import spray.json.DefaultJsonProtocol
import com.github.t3hnar.bcrypt._

import scala.concurrent.Await
import scala.concurrent.duration._

trait BaseSpec extends WordSpec with BeforeAndAfter with BeforeAndAfterAll with Matchers with ScalaFutures with GeneratorDrivenPropertyChecks {
  val config = ConfigFactory.load()

  val adminAcc     = Account("00000", "admin", "password", Role.Admin, approve = true)
  val instAliceAcc = Account("ali840", "alice123", "password", Role.Instructor, approve = true)
  val asstBobAcc   = Account("b123", "bob123", "password", Role.Assistant, approve = true)
  val asstEveAcc   = Account("e123", "eve123", "password", Role.Assistant, approve = false)

  val instAlice = Instructor(instAliceAcc.netId, "alice@gmail.com", "A", "Alice")
  val asstBob   = Assistant(asstBobAcc.netId, 10.50, "bob@gmail.com", Job.Teaching, "CSE", "B", "bob", "1000", "", "", .4)
  val asstEve   = Assistant(asstEveAcc.netId, 10.50, "eve@gmail.com", Job.Grading, "CSE", "E", "eve", "1001", "", "", .4)

  def initDataBase(): Unit = DB.createSchema()

  def initDataBaseData(): Unit = {
    Account.add(
      adminAcc.copy(passwd = adminAcc.passwd.bcrypt),
      instAliceAcc.copy(passwd = instAliceAcc.passwd.bcrypt),
      asstBobAcc.copy(passwd = asstBobAcc.passwd.bcrypt),
      asstEveAcc.copy(passwd = asstEveAcc.passwd.bcrypt)
    )
    instAlice.create()
    asstBob.create()
    asstEve.create()

    import com.utamars.util.TimeImplicits._
    ClockInOutRecord(None, "b123", new DateTime(2015, 9, 4, 11, 45), Some(new DateTime(2015, 9, 4, 13, 15)), Some("incomp"), Some("outcomp")).create()
    ClockInOutRecord(None, "b123", new DateTime(2015, 9, 2, 13, 0), Some(new DateTime(2015, 9, 2, 14, 0)), Some("incomp"), Some("outcomp")).create()
    ClockInOutRecord(None, "b123", new DateTime(2015, 9, 12, 14, 0), Some(new DateTime(2015, 9, 12, 18, 0)), Some("incomp"), Some("outcomp")).create()
    // INSERT INTO clock_in_out_record VALUES (1, 'b123', date '2015-09-04' + TIME '11:45', date '2015-09-04' + TIME '13:15', 'incomp', 'outcomp')
    // INSERT INTO clock_in_out_record VALUES (2, 'b123', date '2015-09-02' + TIME '13:00', date '2015-09-02' + TIME '14:00', 'incomp', 'outcomp')
    // INSERT INTO clock_in_out_record VALUES (3, 'b123', date '2015-09-12' + TIME '14:00', date '2015-09-12' + TIME '18:00', 'incomp', 'outcomp')
  }

  def deleteAllDataBaseData(): Unit = {
    Await.ready(ClockInOutRecord.deleteAll().value, 1.minute)
    Await.ready(Instructor.deleteAll().value, 1.minute)
    Await.ready(Assistant.deleteAll().value, 1.minute)
    Await.ready(Account.deleteAll().value, 1.minute)
  }

  def deleteDataBase(): Unit = DB.dropSchema()
}

trait ServiceSpec extends BaseSpec with ScalatestRouteTest with DefaultJsonProtocol {
  val sessionConfig = SessionConfig.default(SessionUtil.randomServerSecret()).copy(
    sessionMaxAgeSeconds = Some(30),
    refreshTokenMaxAgeSeconds = 1.minute.toSeconds
  )
  implicit val sessionManager = new SessionManager[Username](sessionConfig)
  implicit val sessionStorage = new InMemoryRefreshTokenStorage[Username] {
    override def log(msg: String): Unit = println(msg)
  }

  def requestWithCredentials(request: HttpRequest, user: String, pass: String)(route: Route): RouteTestResult =
    request ~> addCredentials(BasicHttpCredentials(user, pass)) ~> route

  def requestWithCredentials(user: String, pass: String, route: Route)(request: HttpRequest): RouteTestResult =
    request ~> addCredentials(BasicHttpCredentials(user, pass)) ~> route

  def requestWithCredentials(request: HttpRequest, route: Route)(user: String, pass: String): RouteTestResult =
    request ~> addCredentials(BasicHttpCredentials(user, pass)) ~> route
}