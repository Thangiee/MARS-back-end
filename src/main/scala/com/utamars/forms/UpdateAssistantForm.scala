package com.utamars.forms

case class UpdateAssistantForm(
  rate: Option[Double],
  department: Option[String],
  title: Option[String],
  titleCode: Option[String],
  threshold: Option[Double]
) {
  threshold.foreach(value => require(value >= 0 && value <= 1, "'threshold' must be between [0,1]"))
}
