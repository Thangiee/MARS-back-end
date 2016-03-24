package com.utamars

import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import cats.data.XorT
import com.softwaremill.session.{InMemoryRefreshTokenStorage, SessionConfig, SessionManager, SessionUtil}
import com.typesafe.config.ConfigFactory
import com.utamars.api.{Api, Username}
import com.utamars.dataaccess._
import com.utamars.util.FacePP
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}
import spray.json.DefaultJsonProtocol
import spray.json._
import com.github.t3hnar.bcrypt._

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

trait BaseSpec extends WordSpec with BeforeAndAfter with BeforeAndAfterAll with Matchers with ScalaFutures with GeneratorDrivenPropertyChecks {
  val config = ConfigFactory.load()

  val adminAcc     = Account("00000", "admin", "password", Role.Admin, approve = true)
  val instAliceAcc = Account("ali840", "alice123", "password", Role.Instructor, approve = true)
  val asstBobAcc   = Account("b123", "bob123", "password", Role.Assistant, approve = true)
  val asstEveAcc   = Account("e123", "eve123", "password", Role.Assistant, approve = false)

  val admin     = Instructor(adminAcc.netId, "admin@gmail.com", "A", "admin")
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
    admin.create()
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

trait ApiSpec extends BaseSpec with ScalatestRouteTest with util.Implicits {
  val sessionConfig = SessionConfig.default(SessionUtil.randomServerSecret()).copy(
    sessionMaxAgeSeconds = Some(30),
    refreshTokenMaxAgeSeconds = 1.minute.toSeconds
  )
  implicit val sessionManager = new SessionManager[Username](sessionConfig)
  implicit val sessionStorage = new InMemoryRefreshTokenStorage[Username] {
    override def log(msg: String): Unit = println(msg)
  }

  implicit val rejectionHandler = Boot.myRejectionHandler

  class MockFacePP extends FacePP {
    def personDelete(personName: String)(implicit ec: ExeCtx): XorT[Future, HttpResponse, Unit] = ???
    def personCreate(personName: String)(implicit ec: ExeCtx): XorT[Future, HttpResponse, Unit] = ???
    def recognitionVerify(personName: String, faceId: String)(implicit ec: ExeCtx): XorT[Future, HttpResponse, (Confidence, IsSamePerson)] = ???
    def personRemoveFace(personName: String, faceId: String)(implicit ec: ExeCtx): XorT[Future, HttpResponse, Unit] = ???
    def trainVerify(personName: String)(implicit ec: ExeCtx): XorT[Future, HttpResponse, Unit] = ???
    def personAddFace(personName: String, faceId: String)(implicit ec: ExeCtx): XorT[Future, HttpResponse, Unit] = ???
    def detectionDetect(url: String)(implicit ec: ExeCtx): XorT[Future, HttpResponse, FaceId] = ???
  }

  def api: Api

  val adminRequest = requestWithCredentials(adminAcc)(_)
  val instRequest  = requestWithCredentials(instAliceAcc)(_)
  val asstRequest  = requestWithCredentials(asstBobAcc)(_)

  def requestWithCredentials(acc: Account)(request: Account => HttpRequest): RouteTestResult =
    request(acc) ~> addCredentials(BasicHttpCredentials(acc.username, acc.passwd)) ~> seal(api.route)

  def responseTo[T: JsonReader](implicit a: FromResponseUnmarshaller[String], b: ClassManifest[String]): T =
    responseAs[String].parseJson.convertTo[T]

  import spray.json.lenses.JsonLenses._
  def responseToSeq[T: JsonReader](symbol: Symbol)(implicit a: FromResponseUnmarshaller[String], b: ClassManifest[String]): Seq[T] =
    responseAs[String].parseJson.extract[JsValue](symbol / *).map(_.convertTo[T])
}