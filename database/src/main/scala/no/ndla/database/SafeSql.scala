package no.ndla.database

import cats.implicits.*
import scalikejdbc.*

import scala.util.{Success, Try}

private case class SafeSql(sql: SQL[Nothing, NoExtractor]) {
  def map[A](f: WrappedResultSet => A)(using DBSession): Try[List[A]] = Try(sql.map(f).list())

  def mapSingle[A](f: WrappedResultSet => A)(using DBSession): Try[Option[A]] = Try(sql.map(f).single())

  def flatMap[A](f: WrappedResultSet => Try[A])(using DBSession): Try[List[A]] = Try {
    sql.foldLeft[Try[List[A]]](Success(List.empty)) { (acc, rs) =>
      acc.flatMap { list =>
        f(rs).map { a =>
          a +: list
        }
      }
    }
  }.flatten

  def flatMapSingle[A](f: WrappedResultSet => Try[A])(using DBSession): Try[Option[A]] =
    Try(sql.map(f).single().sequence).flatten

  def update()(using DBSession): Try[Int] = Try(sql.update())

  def updateAndReturnGeneratedKey()(using DBSession): Try[Long] = Try(sql.updateAndReturnGeneratedKey())
}

object SafeSql {
  extension (sc: StringContext) {
    def tsql[A](args: Any*): SafeSql = SafeSql(sc.sql(args))
  }
}
