package com.utamars.dataaccess

import cats.data.Xor
import org.squeryl.{CanLookup, KeyedEntityDef, Table}

trait Repo {
  type PK
  type T
  type KED = KeyedEntityDef[T, PK]

  def table: Table[T]

  def create(t: T): Xor[DataAccessErr, T] = withErrHandling(table.insert(t))

  def find(pk: PK)(implicit ev: KED, ev2: PK => CanLookup): Xor[DataAccessErr, T] = withErrHandlingOpt(table.lookup(pk))

  def update(t: T)(implicit ev: KED): Xor[DataAccessErr, T] = withErrHandling { table.update(t); t }

  def delete(pk: PK)(implicit ev: KED, ev2: PK => CanLookup): Xor[DataAccessErr, T] =
    find(pk).flatMap(t => withErrHandling { table.delete(pk); t })

  def all: Xor[DataAccessErr, Iterable[T]] = withErrHandling(table.allRows)

  implicit class RepoPostfixOps(t: T) {
    def insert(implicit ev: KED) = create(t)
    def save(implicit ev: KED) = update(t)
  }
}
