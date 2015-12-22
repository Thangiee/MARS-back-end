package com.utamars.api

import akka.http.scaladsl.model.{MediaTypes, ContentType, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.utamars.dataaccess.Role

case class FacialRecognitionApi() extends Api {

  override val defaultAuthzRoles: Seq[Role] = Seq(Role.Instructor, Role.Assistant)
  override val realm            : String    = "mars-app"

  override val route: Route =
    pathPrefix("recognition") {
      logRequestResult("verify face") {
        path("verify-face") {
          complete(???)
        }
      } ~
        logRequestResult("add face(s) to current account") {
          path("add-face") {
            ???
          }
        } ~
        logRequestResult("remove face(s) from current account") {
          path("remove-face") {
            ???
          }
        }
    } ~
      logRequestResult("retrain current account data set of faces") {
        path("retrain") {
          ???
        }
      }
}
