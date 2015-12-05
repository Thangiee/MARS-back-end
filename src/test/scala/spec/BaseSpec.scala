package spec

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.ConfigFactory
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, Matchers, WordSpec}
import spray.json.DefaultJsonProtocol
import scala.concurrent.duration._
import scala.concurrent.Await
import scalacache._
import com.utamars.dataaccess._

import scalacache.ScalaCache
import scalacache.guava.GuavaCache

trait BaseSpec extends WordSpec with BeforeAndAfter with BeforeAndAfterAll with Matchers with GeneratorDrivenPropertyChecks {
  implicit val scalaCache = ScalaCache(GuavaCache())
}

trait ServiceSpec extends BaseSpec with ScalatestRouteTest with DefaultJsonProtocol {
  val config = ConfigFactory.load()

  val adminAcc           = Account("admin", "password", Role.Admin)
  val instructorAliceAcc = Account("alice123", "password", Role.Instructor)
  val assistantBobAcc    = Account("bob123", "password", Role.Assistant)

  val instructorAlice = Instructor("alice", "A", instructorAliceAcc.username, "alice@gmail.com")
  val assistantBob    = Assistant("1000", assistantBobAcc.username, "albumName", "albumKey", "bob", "B", "bob@gmail.com")

  def clearCache = Await.ready(removeAll(), 5.seconds)

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