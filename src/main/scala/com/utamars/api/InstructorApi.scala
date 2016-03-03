package com.utamars.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import cats.data.XorT
import cats.std.all._
import com.github.t3hnar.bcrypt._
import com.utamars.dataaccess._
import com.utamars.forms.{CreateInstructorAccForm, UpdateInstructorForm}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

case class InstructorApi(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  override val defaultAuthzRoles = Seq(Role.Admin, Role.Instructor, Role.Assistant)

  override val route =
    (post & path("account"/"instructor")) {                                                   // Create instructor account
      formFields('net_id, 'user, 'pass, 'email, 'first, 'last).as(CreateInstructorAccForm) { form =>
        complete(Account.createFromForm(form.copy(pass = form.pass.bcrypt)).reply(_ => OK))
      }
    } ~
    ((post|put) & path("account"/"instructor"/"change-role"/Segment) & authnAndAuthz(Role.Admin)) { (netId, _) =>
      formField('is_admin.as[Boolean]) { isAdmin =>
        val result = for {
          acc    <- Account.findByNetId(netId).leftMap(err2HttpResp)
          isInst  = acc.role == Role.Admin || acc.role == Role.Instructor
          newRole = if (isAdmin) Role.Admin else Role.Instructor
          acc2   <- if (isInst) acc.copy(role = newRole).update().leftMap(err2HttpResp)
                 else XorT.left[Future, HttpResponse, Account](Future.successful((BadRequest, "Require an instructor account")))
        } yield acc2

        complete(result.reply(acc => acc.copy(passwd = "").jsonCompat))
      }
    } ~
    (get & path("instructor") & authnAndAuthz(Role.Admin, Role.Instructor)) { acc =>          // get current instructor info
      complete(Instructor.findByNetId(acc.netId).reply(inst => inst.jsonCompat))
    } ~
    (get & path("instructor"/) & netIdsParam & authnAndAuthz(Role.Admin)) { (ids, _) =>       // Get instructors info by net ids
      complete(Instructor.findByNetIds(ids.toSet).reply(inst => Map("instructors" -> inst).jsonCompat))
    } ~
    (get & path("instructor"/"all") & authnAndAuthz(Role.Admin)) { _ =>                       // get all instructors info
      complete(Instructor.all().reply(inst => Map("instructors" -> inst).jsonCompat))
    } ~
    (get & path("instructor"/Segment) & authnAndAuthz(Role.Admin)) { (netId, _) =>            // get instructor info by netId
      complete(Instructor.findByNetId(netId).reply(inst => inst.jsonCompat))
    } ~
    ((post|put) & path("instructor") & authnAndAuthz(Role.Admin, Role.Instructor)) { acc =>   // Update current instructor info
      formFields('email.?, 'last_name.?, 'first_name.?).as(UpdateInstructorForm) { form =>
        complete(Instructor.update(acc.netId, form).reply(_ => OK))
      }
    }
}