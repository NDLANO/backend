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

trait TrySqlOps[Output, Extractor <: WithExtractor] {
  val underlying: SQL[Output, Extractor]

  private type ThisHasExtractor = SQL[Output, Extractor] =:= SQL[Output, HasExtractor]

  /** Run the SQL query to a list. Corresponds to [[SQL.list]]. */
  def runList()(using ev: ThisHasExtractor, session: DBSession): Try[List[Output]] =
    Try.throwIfInterrupted(ev(underlying).list())

  /** Run the SQL query to a list, and flatten the result to a single `Try`. Corresponds to [[SQL.list]]. */
  def runListFlat[U]()(using
      isTry: Output <:< Try[U],
      hasExtractor: ThisHasExtractor,
      session: DBSession,
  ): Try[List[U]] = Try.throwIfInterrupted(hasExtractor(underlying).list().sequence).flatten

  /** Run the SQL query to an `Option` of the first row. Corresponds to [[SQL.first]]. */
  def runFirst()(using ev: ThisHasExtractor, session: DBSession): Try[Option[Output]] =
    Try.throwIfInterrupted(ev(underlying).first())

  /** Run the SQL query to an `Option` of the first row, and flatten with the inner `Try`. Corresponds to [[SQL.first]].
    */
  def runFirstFlat[U]()(using
      isTry: Output <:< Try[U],
      hasExtractor: ThisHasExtractor,
      session: DBSession,
  ): Try[Option[U]] = Try.throwIfInterrupted(hasExtractor(underlying).first().sequence).flatten

  /** Run the SQL query to a single `Option`. Corresponds to [[SQL.single]]. */
  def runSingle()(using ev: ThisHasExtractor, session: DBSession): Try[Option[Output]] =
    Try.throwIfInterrupted(ev(underlying).single())

  /** Run the SQL query to a single `Option`, and flatten with the inner `Try`. Corresponds to [[SQL.single]]. */
  def runSingleFlat[U]()(using
      isTry: Output <:< Try[U],
      hasExtractor: ThisHasExtractor,
      session: DBSession,
  ): Try[Option[U]] = Try.throwIfInterrupted(hasExtractor(underlying).single().sequence).flatten

  /** Run the SQL query to a single `Try`, producing a `Failure` of the given `Throwable` if the result was empty.
    * Corresponds to [[SQL.single]].
    */
  def runSingleTry(ex: => Throwable)(using ev: ThisHasExtractor, session: DBSession): Try[Output] = Try
    .throwIfInterrupted(ev(underlying).single().toTry(ex))
    .flatten
}

final case class TrySql[Output, Extractor <: WithExtractor](override val underlying: SQL[Output, Extractor])
    extends TrySqlOps[Output, Extractor] {

  /** Execute the SQL statement. Corresponds to [[SQL.execute]]. */
  def execute()(using session: DBSession): Try[Boolean] = Try.throwIfInterrupted(underlying.execute())

  /** Execute the SQL statement, returning the number of updated rows. Corresponds to [[SQL.update]]. */
  def update()(using session: DBSession): Try[Int] = Try.throwIfInterrupted(underlying.update())

  /** Execute the SQL statement, returning the generated key of the inserted value. Corresponds to
    * [[SQL.updateAndReturnGeneratedKey]].
    */
  def updateAndReturnGeneratedKey()(using session: DBSession): Try[Long] =
    Try.throwIfInterrupted(underlying.updateAndReturnGeneratedKey())

  def map[A](f: WrappedResultSet => A): TrySql[A, HasExtractor] = TrySql(underlying.map(f))

  def foldLeft[A](z: A)(f: (A, WrappedResultSet) => A)(using session: DBSession): Try[A] =
    Try.throwIfInterrupted(underlying.foldLeft(z)(f))

  def one[Z](f: WrappedResultSet => Output): TryOneToXSql[Output, Extractor, Z] = TryOneToXSql(underlying.one(f))
}

object TrySql {
  extension (sc: StringContext) {
    def tsql[A](args: Any*): TrySql[A, NoExtractor] = TrySql(sc.sql(args*))
  }
}

final case class TryOneToXSql[One, Extractor <: WithExtractor, Output](underlying: OneToXSQL[One, Extractor, Output]) {

  def toOne[B](to: WrappedResultSet => B): TryOneToOneSql[One, B, Extractor, Output] =
    TryOneToOneSql(underlying.toOne(to))

  def toMany[B](to: WrappedResultSet => Option[B]): TryOneToManySql[One, B, Extractor, Output] =
    TryOneToManySql(underlying.toMany(to))

  def toManies[B1, B2, B3](
      to1: WrappedResultSet => Option[B1],
      to2: WrappedResultSet => Option[B2],
      to3: WrappedResultSet => Option[B3],
  ): TryOneToManies3Sql[One, B1, B2, B3, Extractor, Output] = TryOneToManies3Sql(underlying.toManies(to1, to2, to3))
}

final case class TryOneToOneSql[One, OtherOne, Extractor <: WithExtractor, Output](
    override val underlying: OneToOneSQL[One, OtherOne, Extractor, Output]
) extends TrySqlOps[Output, Extractor] {

  def map(f: (One, OtherOne) => Output): TryOneToOneSql[One, OtherOne, HasExtractor, Output] =
    TryOneToOneSql(underlying.map(f))
}

final case class TryOneToManySql[One, Many, Extractor <: WithExtractor, Output](
    override val underlying: OneToManySQL[One, Many, Extractor, Output]
) extends TrySqlOps[Output, Extractor] {

  def map(f: (One, scala.collection.Seq[Many]) => Output): TryOneToManySql[One, Many, HasExtractor, Output] =
    TryOneToManySql(underlying.map(f))
}

final case class TryOneToManies3Sql[One, Many1, Many2, Many3, Extractor <: WithExtractor, Output](
    override val underlying: OneToManies3SQL[One, Many1, Many2, Many3, Extractor, Output]
) extends TrySqlOps[Output, Extractor] {

  def map(
      f: (One, scala.collection.Seq[Many1], scala.collection.Seq[Many2], scala.collection.Seq[Many3]) => Output
  ): TryOneToManies3Sql[One, Many1, Many2, Many3, HasExtractor, Output] = TryOneToManies3Sql(underlying.map(f))
}
