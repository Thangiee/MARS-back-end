package spec

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.softwaremill.session.{InMemoryRefreshTokenStorage, SessionConfig, SessionManager, SessionUtil}
import com.typesafe.config.ConfigFactory
import com.utamars.api.Username
import com.utamars.dataaccess._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._

trait BaseSpec extends WordSpec with BeforeAndAfter with BeforeAndAfterAll with Matchers with ScalaFutures with GeneratorDrivenPropertyChecks

trait ServiceSpec extends BaseSpec with ScalatestRouteTest with DefaultJsonProtocol {
  val config = ConfigFactory.load()
  val sessionConfig = SessionConfig.default(SessionUtil.randomServerSecret()).copy(
    sessionMaxAgeSeconds = Some(30),
    refreshTokenMaxAgeSeconds = 1.minute.toSeconds
  )
  implicit val sessionManager = new SessionManager[Username](sessionConfig)
  implicit val sessionStorage = new InMemoryRefreshTokenStorage[Username] {
    override def log(msg: String): Unit = println(msg)
  }

  val adminAcc           = Account("admin", "password", Role.Admin)
  val instructorAliceAcc = Account("alice123", "password", Role.Instructor)
  val assistantBobAcc    = Account("bob123", "password", Role.Assistant)

  val instructorAlice = Instructor("alice", "A", instructorAliceAcc.username, "alice@gmail.com")
  val assistantBob    = Assistant("1000", assistantBobAcc.username, "albumName", "albumKey", "bob", "B", "bob@gmail.com")

  def requestWithCredentials(request: HttpRequest, user: String, pass: String)(route: Route): RouteTestResult =
    request ~> addCredentials(BasicHttpCredentials(user, pass)) ~> route

  def requestWithCredentials(user: String, pass: String, route: Route)(request: HttpRequest): RouteTestResult =
    request ~> addCredentials(BasicHttpCredentials(user, pass)) ~> route

  def requestWithCredentials(request: HttpRequest, route: Route)(user: String, pass: String): RouteTestResult =
    request ~> addCredentials(BasicHttpCredentials(user, pass)) ~> route

  def initDataBase(): Unit = transaction {
    MySchema.drop
    MySchema.create
    transaction {
      adminAcc.insert
      instructorAliceAcc.insert
      assistantBobAcc.insert
      instructorAlice.insert
      assistantBob.insert
    }
  }
}