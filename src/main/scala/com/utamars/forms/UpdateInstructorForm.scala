package com.utamars.forms

case class UpdateInstructorForm(
  email: Option[String],
  lastName: Option[String],
  firstName: Option[String]
)

