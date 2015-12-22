package com.utamars.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.config.ConfigFactory
import com.github.nscala_time.time.Imports._
import spray.json._

import scalacache._

case class RegisterUUIDApi(implicit cache: ScalaCache) extends Api {

  private val config = ConfigFactory.load()
  private val ttl = config.getInt("service.registerUUID.ttl-in-sec").seconds

  override val route: Route = logRequestResult("Register UUID") {
    path("register-uuid") {
      (post & formField('uuid)) { (uuid) =>
        complete {
          if (isValidUUID(uuid)) {
            sync.cachingWithTTL(uuid)(ttl)("")
            Map(
              "ttl" -> ttl.toMillis,
              "expireTime" -> (System.currentTimeMillis() + ttl.toMillis)
            ).toJson.compactPrint
          } else {
            HttpResponse(status = 400, entity = "Invalid UUID")
          }
        }
      }
    }
  }

  private def isValidUUID(uuid: String): Boolean = uuid.matches("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
}

