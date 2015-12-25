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
  titleCode: String
)
