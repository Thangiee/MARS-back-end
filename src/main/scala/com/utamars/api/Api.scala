package com.utamars.api

import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.Credentials
import cats.data.Xor
import cats.std.all._
import com.github.nscala_time.time.Imports._
import com.softwaremill.session.SessionDirectives._
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess._

import scala.concurrent.{ExecutionContext, Await, Future}

trait Api extends AnyRef with LazyLogging {

  /** Limit this service to only the included roles */
  def defaultAuthzRoles: Seq[Role] = Nil

  def realm: String = "secure"

  /** Authenticate the account, i.e. check the username and password or an existing session */
  def authn(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS): Directive1[Account] = {
    val authenticator = (credentials: Credentials) => credentials match {
      case p@Credentials.Provided(username) =>
        Account.findBy(username).fold(
          err => { logger.info(err.toString); None },
          acc => if (p.verify(acc.passwd)) Some(acc) else None
        )
      case _ => Future.successful(None)
    }

    authenticateBasicAsync(realm, authenticator).recoverPF {
      case AuthenticationFailedRejection(CredentialsMissing, _) :: _ =>
        // since no credentials were provided, check for a session associated with the request
        requiredSession[String](refreshable[String], usingCookies).flatMap(username =>
          Await.result(Account.findBy(username).value, 1.minute) match {
            case Xor.Right(acc) => provide(acc)
            case Xor.Left(err) => reject(AuthenticationFailedRejection(CredentialsRejected, challengeFor(realm)))
          })
    }
  }

  /** Authorize the account by checking if the user's role is in [[Api#authzRoles]] */
  def authz(acc: Account, authzRoles: Seq[Role]=defaultAuthzRoles): Directive1[Account] =
    if (authzRoles contains acc.role) provide(acc) else reject(AuthorizationFailedRejection)

  /** check authentication and then check Authorization */
  def authnAndAuthz(authzRoles: Role*)(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS): Directive1[Account] =
    authn.flatMap(acc => authz(acc, if (authzRoles.isEmpty) defaultAuthzRoles else authzRoles))

  def route: Route
}

