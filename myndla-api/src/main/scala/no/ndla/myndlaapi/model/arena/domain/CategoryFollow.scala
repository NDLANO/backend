/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.domain

import scalikejdbc._

import scala.util.Try

case class CategoryFollow(
    id: Long,
    user_id: Long,
    category_id: Long
)

object CategoryFollow extends SQLSyntaxSupport[CategoryFollow] {
  override val tableName = "category_follows"

  def fromResultSet(sp: SyntaxProvider[CategoryFollow])(rs: WrappedResultSet): Try[CategoryFollow] =
    fromResultSet(sp.resultName)(rs)

  def fromResultSet(rn: ResultName[CategoryFollow])(rs: WrappedResultSet): Try[CategoryFollow] =
    fromResultSet(rn.c _)(rs)

  def fromResultSet(colFunc: String => SQLSyntax)(rs: WrappedResultSet): Try[CategoryFollow] = Try {
    CategoryFollow(
      id = rs.long(colFunc("id")),
      user_id = rs.long(colFunc("user_id")),
      category_id = rs.long(colFunc("category_id"))
    )
  }
}
