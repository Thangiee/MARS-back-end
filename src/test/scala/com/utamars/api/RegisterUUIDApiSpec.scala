package com.utamars.api

import java.util.UUID

import akka.http.scaladsl.model.{FormData, StatusCodes}
import com.github.nscala_time.time.Imports._
import com.utamars.ServiceSpec
import spray.json._

import scala.concurrent.Await
import scalacache._
import scalacache.guava.GuavaCache


class RegisterUUIDApiSpec extends ServiceSpec {
  implicit val scalaCache = ScalaCache(GuavaCache())
  val service = new RegisterUUIDApi()
  val ttl     = config.getInt("service.registerUUID.ttl-in-sec").seconds.toMillis

  after {
    Await.ready(scalacache.removeAll(), 5.seconds)
  }

  "Register UUID API" when {

    "registering a VALID UUID" must {
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

    "registering a INVALID UUID" must {

      "response with status code bad request (400)" in {
        forAll("Bad UUID") { (badUUID: String) =>
          Post("/register-uuid", FormData("uuid" -> badUUID)) ~> service.route ~> check {
            status shouldEqual StatusCodes.BadRequest
          }
        }
      }
    }

    "verifying that a UUID has been registered" must {
      val uuid = UUID.randomUUID()

      "response with status code 200 if it has been registered" in {
        scalacache.sync.cachingWithTTL(uuid)(2.seconds)("")  // simulate registering the uuid

        Get(s"/register-uuid/verify/$uuid") ~> service.route ~> check {
          status shouldEqual StatusCodes.OK
        }
      }

      "response with status code GONE (410) if it has not been registered" in {
        Get(s"/register-uuid/verify/$uuid") ~> service.route ~> check {
          status shouldEqual StatusCodes.Gone
        }
      }

      "response with status code GONE (410) if it has expired" in {
        scalacache.sync.cachingWithTTL(uuid)(500.millis)("")  // simulate registering the uuid
        Thread.sleep(1.second.millis)
        Get(s"/register-uuid/verify/$uuid") ~> service.route ~> check {
          status shouldEqual StatusCodes.Gone
        }
      }
    }
  }

}
