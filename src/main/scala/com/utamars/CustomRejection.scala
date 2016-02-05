package com.utamars

import akka.http.scaladsl.server.Rejection

object CustomRejection {

  case class NotApprove() extends Rejection
}
