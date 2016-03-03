package com.utamars.api

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import cats.std.all._
import com.github.t3hnar.bcrypt._
import com.utamars.api.DAOs.AssistantDAO
import com.utamars.dataaccess._
import com.utamars.forms.{CreateAssistantForm, UpdateAssistantForm}
import com.utamars.util.FacePP

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

case class AssistantApi(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  override val defaultAuthzRoles = Seq(Role.Admin, Role.Instructor, Role.Assistant)

  override val route =
    (post & path("account"/"assistant")) {                                                          // Create assistant account
      formFields('net_id, 'user, 'pass, 'email, 'rate.as[Double], 'job, 'dept, 'first, 'last,
        'emp_id, 'title, 'title_code, 'threshold.as[Double].?).as(CreateAssistantForm) { form =>

        val result = for {
          _ <- Account.createFromForm(form.copy(pass = form.pass.bcrypt)).leftMap(err2HttpResp)
          _ <- FacePP.personCreate(s"mars_${form.netId}")
        } yield ()

        complete(result.reply(succ => OK, errResp => { Account.deleteByUsername(form.user); errResp }))
      }
    } ~
    (get & path("assistant") & authnAndAuthz(Role.Assistant)) { acc =>                                  // Get current assistant info
      complete(Assistant.findByNetId(acc.netId).reply(asst => AssistantDAO(asst, acc).jsonCompat))
    } ~
    (get & path("assistant"/) & netIdsParam & authnAndAuthz(Role.Admin, Role.Instructor)) { (ids, _) =>  // Get assistants info by net ids
      complete(Assistant.findByNetIdsWithAcc(ids.toSet).reply(assts => Map("assistants" -> assts.map(AssistantDAO(_))).jsonCompat))
    } ~
    (get & path("assistant"/"all") & authnAndAuthz(Role.Admin, Role.Instructor)) { _ =>                 // Get all assistants info
      complete(Assistant.allWithAcc().reply(assts => Map("assistants" -> assts.map(AssistantDAO(_))).jsonCompat))
    } ~
    (get & path("assistant"/Segment) & authnAndAuthz(Role.Instructor, Role.Admin)) { (netId, _) =>      // Get assistant info by netId
      complete(Assistant.findByNetIdWithAcc(netId).reply(asst => AssistantDAO(asst).jsonCompat))
    } ~
    ((post|put) & path("assistant") & authnAndAuthz(Role.Assistant)) { acc =>                           // Update current assistant info
      formFields('rate.as[Double].?, 'dept.?, 'title.?, 'title_code.?, 'threshold.as[Double].?).as(UpdateAssistantForm) { form =>
        complete(Assistant.update(acc.netId, form).reply(_ => OK))
      }
    }
}