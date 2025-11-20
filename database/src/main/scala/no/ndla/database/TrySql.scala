/*
 * Part of NDLA database
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import cats.implicits.*
import no.ndla.common.implicits.toTry
import scalikejdbc.*

import scala.util.Try

final case class TrySql[A](rawSql: SQL[Nothing, NoExtractor], mapping: WrappedResultSet => A) {
  lazy val mappedSql: SQL[A, HasExtractor] = rawSql.map(mapping)

  def map[B](f: A => B): TrySql[B] = withMapping(mapping.andThen(f))

  private def withMapping[B](f: WrappedResultSet => B): TrySql[B] = TrySql(rawSql, f)

  def runList()(using session: DBSession): Try[List[A]] = Try(mappedSql.list())

  def runListFlat[T]()(using session: DBSession, ev: A <:< Try[T]): Try[List[T]] = Try(mappedSql.list().sequence)
    .flatten

  def runSingle()(using session: DBSession): Try[Option[A]] = Try(mappedSql.single())

  def runSingleTry(ex: Throwable)(using session: DBSession): Try[A] = Try(mappedSql.single().toTry(ex)).flatten

  def execute()(using session: DBSession): Try[Boolean] = Try(rawSql.execute())

  def update()(using session: DBSession): Try[Int] = Try(rawSql.update())

  def updateAndReturnGeneratedKey()(using session: DBSession): Try[Long] = Try(rawSql.updateAndReturnGeneratedKey())
}

object TrySql {
  extension (sc: StringContext) {
    def tsql(args: Any*): TrySql[WrappedResultSet] = TrySql(sc.sql(args*), identity)
  }
}
