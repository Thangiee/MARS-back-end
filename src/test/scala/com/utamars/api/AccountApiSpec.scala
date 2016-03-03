package com.utamars.api

import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.StatusCodes._
import com.utamars.ApiSpec
import com.utamars.dataaccess.{Role, Account}

class AccountApiSpec extends ApiSpec {

  val api = AccountApi()

  override def beforeAll(): Unit = {
    initDataBase()
    initDataBaseData()
  }

  "Any user" must {
    "be able to get their account info" in {
      adminRequest(_ => Get("/account")) ~> check {
        responseTo[Account].netId should equal (adminAcc.netId)
      }
    }
  }

  "Admin user" must {
    "be able to get all account info" in {
      adminRequest(_ => Get("/account/all")) ~> check {
        responseToSeq[Account]('accounts) should not be empty
      }
    }

    "be able to delete account" in {
      val temp = Account("99998", "temp", "password", Role.Assistant, approve = true)
      pending
    }

    "be able to enable/disable account" in {
      adminRequest(_ => Post(s"/account/change-approve/${asstBobAcc.username}", FormData("approve" -> "false"))) ~> check {
        asstRequest(_ => Get("/account")) ~> check {
          status should equal(Forbidden)
        }

        // reset the approval
        adminRequest(_ => Post(s"/account/change-approve/${asstBobAcc.username}", FormData("approve" -> "true"))) ~> check {
          asstRequest(_ => Get("/account")) ~> check {
            status should equal(OK)
          }
        }
      }
    }

    "be able to change any account password" in {
      adminRequest(_ => Post(s"/account/change-password/${asstBobAcc.username}", FormData("new_password" -> "123"))) ~> check {
        asstRequest(_ => Get("/account")) ~> check {
          status should equal(Unauthorized)
        }

        // reset the password
        adminRequest(_ => Post(s"/account/change-password/${asstBobAcc.username}", FormData("new_password" -> asstBobAcc.passwd))) ~> check {
          asstRequest(_ => Get("/account")) ~> check {
            status should equal(OK)
          }
        }
      }
    }
  }
}
