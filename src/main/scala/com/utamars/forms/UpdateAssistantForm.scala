package com.utamars.forms

case class UpdateAssistantForm(
  rate: Option[Double],
  department: Option[String],
  title: Option[String],
  titleCode: Option[String]
)
