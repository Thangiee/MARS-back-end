package com.utamars

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import com.softwaremill.session.{RefreshTokenStorage, SessionManager}

import scala.language.implicitConversions

package object api extends AnyRef {

  type Username = String
  type ErrMsg = String
  type Response = ToResponseMarshallable

  // abbreviation
  type SessMgr = SessionManager[Username]
  type RTS = RefreshTokenStorage[Username]

}
