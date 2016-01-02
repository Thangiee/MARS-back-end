package com.utamars.forms

case class CreateAssistantForm(
  netId: String,
  user: String,
  pass: String,
  email: String,
  rate: Double,
  job: String,
  dept: String,
  firstName: String,
  lastName: String,
  empId: String,
  title: String,
  titleCode: String,
  threshold: Option[Double]
) {
  require(!user.contains(" "), "Username cannot contains space.")
  threshold.foreach(value => require(value >= 0 && value <= 1, "'threshold' must be between [0,1]"))
}
