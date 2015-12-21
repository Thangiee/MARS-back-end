package com.utamars.api

import java.util.UUID

import akka.http.scaladsl.model.{FormData, StatusCodes}
import com.github.nscala_time.time.Imports._
import spec.ServiceSpec
import spray.json._

import scala.concurrent.Await
import scalacache._
import scalacache.guava.GuavaCache


class RegisterUUIDServiceSpec extends ServiceSpec {
  implicit val scalaCache = ScalaCache(GuavaCache())
  val service = new RegisterUUIDService()
  val ttl     = config.getInt("service.registerUUID.ttl-in-sec").seconds.toMillis

  after {
    Await.ready(scalacache.removeAll(), 5.seconds)
  }

  "Register UUID service" when {

    "receiving a POST with form data containing a VALID UUID" must {
      val uuid = UUID.randomUUID().toString
      def post = Post("/register-uuid", FormData("uuid" -> uuid)) ~> service.route
      def regTime = System.currentTimeMillis()

      "response with status code ok (200)" in post ~> check {
        status shouldEqual StatusCodes.OK
      }
      "return the UUID's ttl(in millis) and expireTime(in millis since epoch) as a JSON format" in post ~> check {
        val json = responseAs[String].parseJson.convertTo[Map[String, Long]]
        json.getOrElse("ttl", fail) shouldEqual ttl
        json.getOrElse("expireTime", fail) shouldEqual (regTime + ttl) +- 100
      }
      "hold on to the UUID for the duration specified by service.registerUUID.ttl-in-sec in application.conf" in post ~> check {
        whenReady(get(uuid))(_ shouldBe defined)
        Thread.sleep(ttl)
        whenReady(get(uuid))(_ shouldBe None)
      }
    }

    "receiving a POST with form data containing an INVALID UUID" must {

      "response with status code bad request (400)" in {
        forAll("Bad UUID") { (badUUID: String) =>
          Post("/register-uuid", FormData("uuid" -> badUUID)) ~> service.route ~> check {
            status shouldEqual StatusCodes.BadRequest
          }
        }
      }
    }
  }
}
