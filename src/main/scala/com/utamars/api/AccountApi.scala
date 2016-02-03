package com.utamars.api

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import cats.std.all._
import com.github.t3hnar.bcrypt._
import com.utamars.dataaccess._
import com.utamars.forms.{CreateAssistantForm, CreateInstructorAccForm, UpdateAssistantForm, UpdateInstructorForm}
import com.utamars.util.FacePP

import scala.concurrent.ExecutionContext

case class AccountApi(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  override val defaultAuthzRoles = Seq(Role.Admin, Role.Instructor, Role.Assistant)

  override val route =
    (post & path("account"/"assistant")) {
      formFields('netid, 'user, 'pass, 'email, 'rate.as[Double], 'job, 'dept, 'first, 'last,
        'empid, 'title, 'titlecode, 'threshold.as[Double].?).as(CreateAssistantForm) { form =>

        val result = for {
          _ <- Account.createFromForm(form.copy(pass = form.pass.bcrypt)).leftMap(err2HttpResp)
          _ <- FacePP.personCreate(s"mars_${form.netId}")
        } yield ()

        complete(result.reply(succ => OK, errResp => { Account.deleteBy(form.user); errResp }))
      }
    } ~
    (post & path("account"/"instructor")) {
      formFields('netid, 'user, 'pass, 'email, 'first, 'last).as(CreateInstructorAccForm) { form =>
        complete(Account.createFromForm(form.copy(pass = form.pass.bcrypt)).reply(_ => OK))
      }
    } ~
    (get & path("account") & authnAndAuthz()) { (acc) =>
      complete(Account.findBy(acc.username).reply(acc => acc.copy(passwd = "").jsonCompat)) // hide password
    } ~
    (get & path("account"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) =>
      complete(Account.findBy(username).reply(acc => acc.copy(passwd = "").jsonCompat))
    } ~
    (delete & path("account"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) =>
      val result = for {
        acc <- Account.findBy(username).leftMap(err2HttpResp)
        _   <- FacePP.personDelete(s"mars_${acc.netId}")
        _   <- Account.deleteBy(username).leftMap(err2HttpResp)
      } yield ()

      complete(result.reply(_ => OK))
    } ~
    ((post|put) & path("account"/"change-password") & authnAndAuthz()) { acc =>
      formField('newpassword) { newPass =>
        complete(acc.changePassword(newPass.bcrypt).reply(_ => OK))
      }
    } ~
    ((post|put) & path("account"/"change-password"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) =>
      formField('newpassword) { newPass =>
        complete(Account.changePassword(username, newPass.bcrypt).reply(_ => OK))
      }
    } ~
    (get & path("assistant") & authnAndAuthz(Role.Assistant)) { acc =>
      complete(Assistant.findBy(acc.netId).reply(asst => asst.jsonCompat))
    } ~
    (get & path("assistant"/Segment) & authnAndAuthz(Role.Instructor, Role.Admin)) { (netId, _) =>
      complete(Assistant.findBy(netId).reply(asst => asst.jsonCompat))
    } ~
    ((post|put) & path("assistant") & authnAndAuthz(Role.Assistant)) { acc =>
      formFields('rate.as[Double].?, 'dept.?, 'title.?, 'titlecode.?, 'threshold.as[Double].?).as(UpdateAssistantForm) { form =>
        complete(Assistant.update(acc.netId, form).reply(_ => OK))
      }
    } ~
    (get & path("instructor") & authnAndAuthz(Role.Instructor)) { acc =>
      complete(Instructor.findBy(acc.netId).reply(inst => inst.jsonCompat))
    } ~
    (get & path("instructor"/Segment) & authnAndAuthz(Role.Admin)) { (netId, _) =>
      complete(Instructor.findBy(netId).reply(inst => inst.jsonCompat))
    } ~
    ((post|put) & path("instructor") & authnAndAuthz(Role.Instructor)) { acc =>
      formFields('email.?, 'lastname.?, 'firstname.?).as(UpdateInstructorForm) { form =>
        complete(Instructor.update(acc.netId, form).reply(_ => OK))
      }
    }
}