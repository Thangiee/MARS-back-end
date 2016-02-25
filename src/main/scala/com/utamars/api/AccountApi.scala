package com.utamars.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshaller.CsvSeq
import cats.data.XorT
import cats.std.all._
import com.github.t3hnar.bcrypt._
import com.utamars.dataaccess._
import com.utamars.forms.{CreateAssistantForm, CreateInstructorAccForm, UpdateAssistantForm, UpdateInstructorForm}
import com.utamars.util.FacePP

import scala.concurrent.{Future, ExecutionContext}
import scala.language.postfixOps

case class AccountApi(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  override val defaultAuthzRoles = Seq(Role.Admin, Role.Instructor, Role.Assistant)

  override val route = accountRoutes ~ assistantRoutes ~ instructorRoutes

  private def netIdsParam = parameter("net-ids".as(CsvSeq[String]))

  private def accountRoutes =
    (get & path("account") & authnAndAuthz()) { (acc) =>                                                    // Get account info
      complete(Account.findByUsername(acc.username).reply(acc => acc.copy(passwd = "").jsonCompat))
    } ~
    (get & path("account"/"all") & authnAndAuthz(Role.Admin)) { _ =>                                        // Get all account info
      complete(Account.all().reply(accs => Map("accounts" -> accs.map(_.copy(passwd = ""))).jsonCompat))
    } ~
    (get & path("account"/) & netIdsParam & authnAndAuthz(Role.Admin)) { (ids, _) =>                        // Get accounts info by net ids
      complete(Account.findByNetIds(ids.toSet).reply(accs => Map("accounts" -> accs.map(_.copy(passwd = ""))).jsonCompat))
    } ~
    (get & path("account"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) =>                          // Get account info by username
      complete(Account.findByUsername(username).reply(acc => acc.copy(passwd = "").jsonCompat))
    } ~
    (delete & path("account"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) =>                       // Delete account by username
      val result = for {
        acc <- Account.findByUsername(username).leftMap(err2HttpResp)
        _   <- FacePP.personDelete(s"mars_${acc.netId}")
        _   <- Account.deleteByUsername(username).leftMap(err2HttpResp)
      } yield ()

      complete(result.reply(_ => OK))
    } ~
    ((post|put) & path("account"/"change-password") & authnAndAuthz()) { acc =>                             // Change account password
      formField('new_password) { newPass =>
        complete(acc.changePassword(newPass.bcrypt).reply(_ => OK))
      }
    } ~
    ((post|put) & path("account"/"change-password"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) => // Change account password by username
      formField('new_password) { newPass =>
        complete(Account.changePassword(username, newPass.bcrypt).reply(_ => OK))
      }
    } ~
    ((post|put) & path("account"/"change-approve"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) => // Change account approval by username
      formField('approve.as[Boolean]) { newApprove =>
        complete(Account.findByUsername(username).flatMap(_.copy(approve = newApprove).update()).reply(_ => OK))
      }
    }

  private def assistantRoutes =
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
      complete(Assistant.findByNetId(acc.netId).reply(asst => asst.jsonCompat))
    } ~
    (get & path("assistant"/) & netIdsParam & authnAndAuthz(Role.Admin, Role.Instructor)) { (ids, _) =>  // Get assistants info by net ids
      complete(Assistant.findByNetIds(ids.toSet).reply(assts => Map("assistants" -> assts).jsonCompat))
    } ~
    (get & path("assistant"/"all") & authnAndAuthz(Role.Admin, Role.Instructor)) { _ =>                 // Get all assistants info
      complete(Assistant.all().reply(assts => Map("assistants" -> assts).jsonCompat))
    } ~
    (get & path("assistant"/Segment) & authnAndAuthz(Role.Instructor, Role.Admin)) { (netId, _) =>      // Get assistant info by netId
      complete(Assistant.findByNetId(netId).reply(asst => asst.jsonCompat))
    } ~
    ((post|put) & path("assistant") & authnAndAuthz(Role.Assistant)) { acc =>                           // Update current assistant info
      formFields('rate.as[Double].?, 'dept.?, 'title.?, 'title_code.?, 'threshold.as[Double].?).as(UpdateAssistantForm) { form =>
        complete(Assistant.update(acc.netId, form).reply(_ => OK))
      }
    }

  private def instructorRoutes =
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