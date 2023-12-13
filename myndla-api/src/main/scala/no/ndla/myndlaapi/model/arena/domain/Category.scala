/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.domain

import scalikejdbc._

import scala.util.Try

case class Category(
    id: Long,
    title: String,
    description: String
)

case class InsertCategory(
    title: String,
    description: String
) {
  def toFull(id: Long): Category = {
    Category(
      id = id,
      title = title,
      description = description
    )
  }
}

object Category extends SQLSyntaxSupport[Category] {
  override val tableName = "categories"

  def fromResultSet(sp: SyntaxProvider[Category])(rs: WrappedResultSet): Try[Category] =
    fromResultSet(sp.resultName)(rs)

  def fromResultSet(rn: ResultName[Category])(rs: WrappedResultSet): Try[Category] = Try {
    Category(
      id = rs.long(rn.c("id")),
      title = rs.string(rn.c("title")),
      description = rs.string(rn.c("description"))
    )
  }

}
