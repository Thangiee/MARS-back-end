package com.utamars.api

import akka.http.scaladsl.server.Route
import com.utamars.dataaccess.Role

class FacialRecognitionService extends Service {

  override def authzRoles: Seq[Role] = Seq(Role.Assistant)

  override def realm: String = super.realm

  override def route: Route = ???
}
