package com.utamars.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{AuthenticationDirective, Credentials}
import cats.data.Xor
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess._
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Service extends AnyRef with DefaultJsonProtocol with LazyLogging {

  /* Limit this service to only the included roles */
  def authzRoles: Seq[Role] = Nil

  def realm: String = ""

  /* Authenticate the account, i.e. check the username and password */
  def authn: AuthenticationDirective[Account] = authenticateBasicAsync(realm, authenticator)

  /* Authorize the account by checking if the user's role is in [[Service#authzRoles]] */
  def authz(acc: Account): Directive1[Account] =
    if (authzRoles contains acc.role) provide(acc) else reject(AuthorizationFailedRejection)

  /* check authentication and then check Authorization */
  def authnAndAuthz: Directive1[Account] = authn.flatMap(authz)

  private def authenticator(credentials: Credentials): Future[Option[Account]] = credentials match {
    case p @ Credentials.Provided(username) => Future {
      transaction(Account.find(username)) match {
        case Xor.Right(acc) => if (p.verify(acc.passwd)) Some(acc) else None
        case Xor.Left(err) => logger.info(err.toString); None
      }
    }
    case _ => Future.successful(None)
  }

  def route: Route
}

