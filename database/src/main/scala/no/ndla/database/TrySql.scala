/*
 * Part of NDLA database
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import cats.implicits.*
import no.ndla.common.TryUtil.throwIfInterrupted
import no.ndla.common.implicits.toTry
import scalikejdbc.*

import scala.util.Try

final case class TrySql[A](rawSql: SQL[Nothing, NoExtractor], mapping: WrappedResultSet => A) {
  lazy val mappedSql: SQL[A, HasExtractor] = rawSql.map(mapping)

  /** Transform the produced values of this SQL query by applying the given function. */
  def map[B](f: A => B): TrySql[B] = withMapping(mapping.andThen(f))

  private def withMapping[B](f: WrappedResultSet => B): TrySql[B] = TrySql(rawSql, f)

  /** Run the SQL query to a list. Corresponds to [[SQL.list]]. */
  def runList()(using session: DBSession): Try[List[A]] = Try.throwIfInterrupted(mappedSql.list())

  /** Run the SQL query to a list, and flatten the result to a single `Try`. Corresponds to [[SQL.list]]. */
  def runListFlat[T]()(using session: DBSession, ev: A <:< Try[T]): Try[List[T]] = Try
    .throwIfInterrupted(mappedSql.list().sequence)
    .flatten

  /** Run the SQL query to a single `Option`. Corresponds to [[SQL.single]]. */
  def runSingle()(using session: DBSession): Try[Option[A]] = Try.throwIfInterrupted(mappedSql.single())

  /** Run the SQL query to a single `Try`, producing a `Failure` of the given `Throwable` if the result was empty.
    * Corresponds to [[SQL.single]].
    */
  def runSingleTry(ex: => Throwable)(using session: DBSession): Try[A] = Try
    .throwIfInterrupted(mappedSql.single().toTry(ex))
    .flatten

  /** Execute the SQL statement. Corresponds to [[SQL.execute]]. */
  def execute()(using session: DBSession): Try[Boolean] = Try.throwIfInterrupted(rawSql.execute())

  /** Execute the SQL statement, returning the number of updated rows. Corresponds to [[SQL.update]]. */
  def update()(using session: DBSession): Try[Int] = Try.throwIfInterrupted(rawSql.update())

  /** Execute the SQL statement, returning the generated key of the inserted value. Corresponds to
    * [[SQL.updateAndReturnGeneratedKey]].
    */
  def updateAndReturnGeneratedKey()(using session: DBSession): Try[Long] =
    Try.throwIfInterrupted(rawSql.updateAndReturnGeneratedKey())
}

object TrySql {
  extension (sc: StringContext) {
    def tsql(args: Any*): TrySql[WrappedResultSet] = TrySql(sc.sql(args*), identity)
  }
}
