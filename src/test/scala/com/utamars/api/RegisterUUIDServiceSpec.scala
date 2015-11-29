package com.utamars.api

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.FormData
import spec.ServiceSpec
import spray.json._


class RegisterUUIDServiceSpec extends ServiceSpec {

  val service = new RegisterUUIDService()
  val ttl     = config.getInt("service.registerUUID.ttl-in-sec").seconds.toMillis

  override def afterAll(): Unit = {
    clearCache
  }

  "Register UUID service" when {

    "receiving a POST with form data containing a VALID UUID" must {
      val uuid = UUID.randomUUID().toString
      val post = Post("/register-uuid", FormData("uuid" -> uuid)) ~> service.route
      val regTime = System.currentTimeMillis()

      "response with status code ok (200)" in post ~> check {
        status shouldEqual StatusCodes.OK
      }
      "return the UUID's ttl(in millis) and expireTime(in millis since epoch) as a JSON format" in post ~> check {
        val json = responseAs[String].parseJson.convertTo[Map[String, Long]]
        json.getOrElse("ttl", fail) shouldEqual ttl
        json.getOrElse("expireTime", fail) shouldEqual (regTime + ttl) +- 100
      }
      "hold on to the UUID for the duration specified by service.registerUUID.ttl-in-sec in application.conf" in post ~> check {
        import scalacache._
        sync.get(uuid) shouldBe defined
        Thread.sleep(ttl)
        sync.get(uuid) shouldBe None
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
