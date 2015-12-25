package com.utamars.api

import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenge, HttpCredentials}
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsRejected
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import cats.data.Xor
import com.github.t3hnar.bcrypt._
import com.softwaremill.session.SessionDirectives._
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success}

trait Api extends AnyRef with LazyLogging {

  /** Limit this service to only the included roles */
  def defaultAuthzRoles: Seq[Role] = Nil

  def realm: String = "secure"

  def checkSession(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS): Directive1[Account] =
    requiredSession[String](refreshable[String], usingCookies).flatMap { username =>
      onComplete(Account.findBy(username).value).flatMap[Tuple1[Account]] {
        case Success(Xor.Right(acc)) => provide(acc)
        case Success(Xor.Left(err)) =>
          logger.info(s"Fail to authenticate due to $err")
          reject(AuthenticationFailedRejection(CredentialsRejected, challengeFor(realm)))
        case _ => reject
      }
    }

  def checkUserAndPass(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS): Directive1[Account] = {
    val challenge = HttpChallenge("MyAuth", realm)
    val authenticator= (credentials: Option[HttpCredentials]) => credentials match {
      case Some(BasicHttpCredentials(providedUser, providedPass)) =>
        Account.findBy(providedUser).value.map {
          case Xor.Right(acc) =>
            if (Try(providedPass.isBcrypted(acc.passwd)).getOrElse(false)) Right(acc) else Left(challenge)
          case Xor.Left(err) => logger.info(s"Fail to find account for Authn due to $err"); Left(challenge)
        }
      case _ => Future.successful(Left(challenge))
    }

    authenticateOrRejectWithChallenge(authenticator)
  }

  /** Authenticate the account, i.e. check the username and password or an existing session */
  def authn(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS): Directive1[Account] = checkUserAndPass | checkSession

  /** Authorize the account by checking if the user's role is in [[Api#authzRoles]] */
  def authz(acc: Account, authzRoles: Seq[Role]=defaultAuthzRoles): Directive1[Account] =
    if (authzRoles contains acc.role) provide(acc) else reject(AuthorizationFailedRejection)

  /** check authentication and then check Authorization */
  def authnAndAuthz(authzRoles: Role*)(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS): Directive1[Account] =
    authn.flatMap(acc => authz(acc, if (authzRoles.isEmpty) defaultAuthzRoles else authzRoles))

  def route: Route
}

