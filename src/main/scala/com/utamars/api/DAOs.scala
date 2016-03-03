package com.utamars.api

import java.sql.Timestamp

import com.utamars.dataaccess.{Account, Assistant}

object DAOs {

  case class AssistantDAO(
    netId: String,
    username: String,
    role: String,
    createTime: Timestamp,
    approve: Boolean,
    rate: Double,
    email: String,
    job: String,
    department: String,
    lastName: String,
    firstName: String,
    employeeId: String,
    title: String,
    titleCode: String,
    threshold: Double
  )

  object AssistantDAO {
    implicit val asstDAO = jsonFormat15(AssistantDAO.apply)

    def apply(data: (Assistant, Account)): AssistantDAO = apply(data._1, data._2)
    def apply(asst: Assistant, acc: Account): AssistantDAO = AssistantDAO(
      acc.netId, acc.username, acc.role, acc.createTime, acc.approve, asst.rate, asst.email, asst.job, asst.department,
      asst.lastName, asst.firstName, asst.employeeId, asst.title, asst.titleCode, asst.threshold
    )
  }
}
