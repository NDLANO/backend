/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.arena.domain

import scalikejdbc._

import scala.util.Try

case class Category(
    id: Long,
    title: String,
    description: String,
    visible: Boolean,
    rank: Int,
    parentCategoryId: Option[Long]
)

case class InsertCategory(
    title: String,
    description: String,
    visible: Boolean,
    parentCategoryId: Option[Long]
)

object Category extends SQLSyntaxSupport[Category] {
  override val tableName = "categories"

  def fromResultSet(sp: SyntaxProvider[Category])(rs: WrappedResultSet): Try[Category] =
    fromResultSet(sp.resultName)(rs)

  def fromResultSet(rn: ResultName[Category])(rs: WrappedResultSet): Try[Category] = fromResultSet(s => rn.c(s))(rs)

  def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): Try[Category] = Try {
    Category(
      id = rs.long(colNameWrapper("id")),
      title = rs.string(colNameWrapper("title")),
      description = rs.string(colNameWrapper("description")),
      visible = rs.boolean(colNameWrapper("visible")),
      rank = rs.int(colNameWrapper("rank")),
      parentCategoryId = rs.longOpt(colNameWrapper("parent_category_id"))
    )
  }

}
