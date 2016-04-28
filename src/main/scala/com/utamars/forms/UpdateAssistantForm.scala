package com.utamars.forms

case class UpdateAssistantForm(
  empId: Option[String],
  rate: Option[Double],
  job: Option[String],
  department: Option[String],
  title: Option[String],
  titleCode: Option[String],
  threshold: Option[Double]
) {
  job.foreach(value => require(Seq("teaching", "grading").contains(value.toLowerCase()), "'job' must be 'teaching' or 'grading'"))
  threshold.foreach(value => require(value >= 0 && value <= 1, "'threshold' must be between [0,1]"))
}
