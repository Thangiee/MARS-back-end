package com.utamars.dataaccess

case class Assistant(
  employeeId: String,
  username: String,
  firstName: String = "",
  lastName: String = "",
  email: String = "",
  title: String = "N/A",
  titleCode: String = "N/A",
  department: String = "CSE",
  rate: Double = 0.0,
  job: Job = Job.Teaching,
  currentlyClockedIn: Boolean = false
)

object Assistant extends Repo {
  override type PK = String
  override type T = Assistant

  override def table = MySchema.assistants

  def findByUsername(username: String) = withErrHandlingOpt(table.where(_.username === username).singleOption)
}
