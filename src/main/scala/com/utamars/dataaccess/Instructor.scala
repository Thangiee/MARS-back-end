package com.utamars.dataaccess

import java.util.UUID

case class Instructor(
  firstName: String,
  lastName: String,
  username: String,
  email: String = "",
  id: UUID = UUID.randomUUID())

object Instructor extends Repo {
  override type PK = UUID
  override type T = Instructor
  override def table = MySchema.instructors
}
