package com.utamars.api

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import com.utamars.dataaccess._
import com.utamars.forms.{CreateAssistantForm, CreateInstructorAccForm, UpdateAssistantForm, UpdateInstructorForm}
import spray.json._
import com.github.t3hnar.bcrypt._

import scala.concurrent.ExecutionContext

case class AccountApi(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  override val defaultAuthzRoles = Seq(Role.Admin, Role.Instructor, Role.Assistant)

  override val route =
    ((post|put) & path("account"/"assistant")) {
      formFields('netid, 'user, 'pass, 'email, 'rate.as[Double], 'job,
        'dept, 'first, 'last, 'empid, 'title, 'titlecode).as(CreateAssistantForm) { form =>
        Account.createFromForm(form.copy(pass = form.pass.bcrypt)).responseWith(OK)
      }
    } ~
    ((post|put) & path("account"/"instructor")) {
      formFields('netid, 'user, 'pass, 'email, 'first, 'last).as(CreateInstructorAccForm) { form =>
        Account.createFromForm(form.copy(pass = form.pass.bcrypt)).responseWith(OK)
      }
    } ~
    (get & path("account") & authnAndAuthz()) { (acc) =>
      Account.findBy(acc.username).responseWith(acc => acc.copy(passwd = "").toJson.compactPrint) // hide password
    } ~
    (get & path("account"/Segment) & authnAndAuthz(Role.Admin, Role.Instructor)) { (username, _) =>
      Account.findBy(username).responseWith(acc => acc.copy(passwd = "").toJson.compactPrint)
    } ~
    ((post|put) & path("account"/"change-password") & authnAndAuthz()) { acc =>
      formField('newpassword) { newPass =>
        acc.changePassword(newPass.bcrypt).responseWith(OK)
      }
    } ~
    ((post|put) & path("account"/"change-password"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) =>
      formField('newpassword) { newPass =>
        Account.changePassword(username, newPass.bcrypt).responseWith(OK)
      }
    } ~
    (get & path("assistant") & authnAndAuthz(Role.Assistant)) { acc =>
      Assistant.findBy(acc.netId).responseWith(asst => asst.toJson.compactPrint)
    } ~
    (get & path("assistant"/Segment) & authnAndAuthz(Role.Instructor, Role.Admin)) { (netId, _) =>
      Assistant.findBy(netId).responseWith(asst => asst.toJson.compactPrint)
    } ~
    ((post|put) & path("assistant") & authnAndAuthz(Role.Assistant)) { acc =>
      formFields('rate.as[Double].?, 'dept.?, 'title.?, 'titlecode.?).as(UpdateAssistantForm) { form =>
        Assistant.update(acc.netId, form).responseWith(OK)
      }
    } ~
    (get & path("instructor") & authnAndAuthz(Role.Instructor)) { acc =>
      Instructor.findBy(acc.netId).responseWith(inst => inst.toJson.compactPrint)
    } ~
    (get & path("instructor"/Segment) & authnAndAuthz(Role.Admin)) { (netId, _) =>
      Instructor.findBy(netId).responseWith(inst => inst.toJson.compactPrint)
    } ~
    ((post|put) & path("instructor") & authnAndAuthz(Role.Instructor)) { acc =>
      formFields('email.?, 'lastname.?, 'firstname.?).as(UpdateInstructorForm) { form =>
        Instructor.update(acc.netId, form).responseWith(OK)
      }
    }
}