package com.utamars.api

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import cats.std.all._
import com.github.t3hnar.bcrypt._
import com.utamars.api.DAOs.AccountDAO
import com.utamars.dataaccess._
import com.utamars.util.FacePP

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

case class AccountApi(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS, facePP: FacePP) extends Api {

  override val defaultAuthzRoles = Seq(Role.Admin, Role.Instructor, Role.Assistant)

  override val route =
    (get & path("account") & authnAndAuthz()) { (acc) =>                                                    // Get account info
      complete(Account.findByNetId(acc.netId).reply(AccountDAO(_).jsonCompat))
    } ~
    (get & path("account"/"all") & authnAndAuthz(Role.Admin)) { _ =>                                        // Get all account info
      complete(Account.all().reply(accs => Map("accounts" -> accs.map(AccountDAO(_))).jsonCompat))
    } ~
    (get & path("account"/) & netIdsParam & authnAndAuthz(Role.Admin)) { (ids, _) =>                        // Get accounts info by net ids
      complete(Account.findByNetIds(ids.toSet).reply(accs => Map("accounts" -> accs.map(AccountDAO(_))).jsonCompat))
    } ~
    (get & path("account"/Segment) & authnAndAuthz(Role.Admin)) { (netId, _) =>                             // Get account info by net id
      complete(Account.findByNetId(netId).reply(AccountDAO(_).jsonCompat))
    } ~
    (delete & path("account"/Segment) & authnAndAuthz(Role.Admin)) { (netId, _) =>                          // Delete account by net id
      val result = for {
        acc <- Account.findByNetId(netId).leftMap(err2HttpResp)
        _   <- facePP.personDelete(s"mars_${acc.netId}")
        _   <- Account.deleteByUsername(acc.username).leftMap(err2HttpResp)
      } yield ()

      complete(result.reply(_ => OK))
    } ~
    ((post|put) & path("account"/"change-password") & authnAndAuthz()) { acc =>                             // Change account password
      formField('new_password) { newPass =>
        complete(acc.changePassword(newPass.bcrypt).reply(_ => OK))
      }
    } ~
    ((post|put) & path("account"/"change-password"/Segment) & authnAndAuthz(Role.Admin)) { (netId, _) =>   // Change account password by net id
      formField('new_password) { newPass =>
        complete(Account.changePassword(netId, newPass.bcrypt).reply(_ => OK))
      }
    } ~
    ((post|put) & path("account"/"change-approve"/Segment) & authnAndAuthz(Role.Admin)) { (netId, _) =>    // Change account approval by net id
      formField('approve.as[Boolean]) { newApprove =>
        complete(Account.findByNetId(netId).flatMap(_.copy(approve = newApprove).update()).reply(_ => OK))
      }
    }
}