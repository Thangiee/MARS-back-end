package com.utamars.api

import akka.http.scaladsl.server.Route
import com.utamars.dataaccess.Role

class TimeSheetGenService extends Service {

  override def authzRoles: Seq[Role] = Seq(Role.Assistant, Role.Instructor)

  override def realm: String = super.realm

  override def route: Route = ???
}
