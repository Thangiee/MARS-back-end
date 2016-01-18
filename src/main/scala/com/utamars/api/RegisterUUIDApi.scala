package com.utamars.api

import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.github.nscala_time.time.Imports._
import spray.json._

import scalacache.ScalaCache

case class RegisterUUIDApi(implicit cache: ScalaCache) extends Api {

  private val ttl = config.getInt("service.registerUUID.ttl-in-sec").seconds

  override val route: Route = logRequestResult("Register UUID") {
    (post & path("register-uuid") & formField('uuid)) { uuid =>
      complete {
        if (isValidUUID(uuid)) {
          scalacache.sync.cachingWithTTL(uuid)(ttl)("")
          Map(
            "ttl" -> ttl.toMillis,
            "expireTime" -> (System.currentTimeMillis() + ttl.toMillis)
          ).toJson.compactPrint
        } else {
          HttpResponse(status = 400, entity = "Invalid UUID")
        }
      }
    } ~
    (get & path("register-uuid" / "verify" / Segment)) { uuid =>
      complete {
        scalacache.sync.get(uuid) match {
          case Some(_) => uuid
          case None    => HttpResponse(StatusCodes.Gone)
        }
      }
    }
  }

  private def isValidUUID(uuid: String): Boolean = uuid.matches("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
}

