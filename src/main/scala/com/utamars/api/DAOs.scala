package com.utamars.api

import java.sql.Timestamp

import com.utamars.dataaccess.{Instructor, Account, Assistant}

object DAOs {

  case class AccountDAO(netId: String, username: String, role: String, createTime: Timestamp, approve: Boolean)

  object AccountDAO {
    def apply(acc: Account): AccountDAO = AccountDAO(acc.netId, acc.username, acc.role, acc.createTime, acc.approve)
  }

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
    def apply(data: (Assistant, Account)): AssistantDAO = apply(data._1, data._2)
    def apply(asst: Assistant, acc: Account): AssistantDAO = AssistantDAO(
      acc.netId, acc.username, acc.role, acc.createTime, acc.approve, asst.rate, asst.email, asst.job, asst.department,
      asst.lastName, asst.firstName, asst.employeeId, asst.title, asst.titleCode, asst.threshold
    )
  }

  case class InstructorDAO(
    netId: String,
    username: String,
    role: String,
    createTime: Timestamp,
    approve: Boolean,
    email: String,
    lastName: String,
    firstName: String
  )

  object InstructorDAO {
    def apply(data: (Instructor, Account)): InstructorDAO = apply(data._1, data._2)
    def apply(inst: Instructor, acc: Account): InstructorDAO = InstructorDAO(
      acc.netId, acc.username, acc.role, acc.createTime, acc.approve, inst.email, inst.lastName, inst.firstName
    )
  }
}
