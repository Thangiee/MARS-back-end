package com.utamars.api

import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenge, HttpCredentials}
import akka.http.scaladsl.model.{StatusCode, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsRejected
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import cats.data.{Xor, XorT}
import cats.std.all._
import com.github.t3hnar.bcrypt._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.typesafe.scalalogging.LazyLogging
import com.utamars.CustomRejection.NotApprove
import com.utamars.dataaccess._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Success, Try}

trait Api extends AnyRef with LazyLogging {

  def route: Route

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
    if (authzRoles contains acc.role) { if (acc.approve) provide(acc) else reject(NotApprove()) }
    else reject(AuthorizationFailedRejection)

  /** check authentication and then check Authorization */
  def authnAndAuthz(authzRoles: Role*)(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS): Directive1[Account] =
    authn.flatMap(acc => authz(acc, if (authzRoles.isEmpty) defaultAuthzRoles else authzRoles))


  implicit class XorHttpResponseImplicit[L <: HttpResponse, R](future: XorT[Future, L, R]) {
    def reply(onSucc: (R) => Response)(implicit ec: ExecutionContext): Future[Response] = future.fold(response => response, onSucc)
    def reply(onSucc: (R) => Response, onErr: (L) => Response)(implicit ec: ExecutionContext): Future[Response] = future.fold(onErr, onSucc)
  }

  implicit class XorDataAccessErrImplicit[L <: DataAccessErr, R](future: XorT[Future, L, R]) {
    def reply(onSucc: (R) => Response)(implicit ec: ExecutionContext) : Future[Response] = future.fold(err => err2HttpResp(err), onSucc)
    def reply(onSucc: (R) => Response, onErr: (L) => Response)(implicit ec: ExecutionContext): Future[Response] = future.fold(onErr, onSucc)
  }

  implicit def tuple2HttpResponse(response: (StatusCode, String)): HttpResponse = {
    val (code, msg) = response
    HttpResponse(code, entity = msg)
  }

  def err2HttpResp(err: DataAccessErr): HttpResponse = err match {
    case NotFound             => HttpResponse(StatusCodes.NotFound)
    case SqlDuplicateKey(msg) => HttpResponse(StatusCodes.Conflict, entity = msg)
    case SqlErr(code, msg) =>
      logger.error(s"SQL error code: $code | $msg")
      HttpResponse(StatusCodes.InternalServerError)
    case InternalErr(error)  =>
      logger.error(error.getMessage)
      error.printStackTrace()
      HttpResponse(StatusCodes.InternalServerError)
  }
}

