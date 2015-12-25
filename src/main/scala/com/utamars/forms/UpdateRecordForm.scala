package com.utamars.forms

case class UpdateRecordForm(
  inTime: Option[Long],
  outTime: Option[Long],
  inComputerId: Option[String] = None,
  outComputerId: Option[String] = None
) {
  inTime.foreach(time => require(time > 0 , "'intime' must be positive"))
  outTime.foreach(time => require(time > 0 , "'outtime' must be positive"))
}
