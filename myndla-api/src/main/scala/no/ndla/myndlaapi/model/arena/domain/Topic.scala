/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.arena.domain

import no.ndla.common.model.NDLADate
import scalikejdbc._

import scala.util.Try

case class Topic(
    id: Long,
    ownerId: Option[Long],
    title: String,
    category_id: Long,
    created: NDLADate,
    updated: NDLADate,
    deleted: Option[NDLADate],
    override val locked: Boolean,
    pinned: Boolean
) extends Owned

object Topic extends SQLSyntaxSupport[Topic] {
  override val tableName: String = "topics"

  def fromResultSet(sp: SyntaxProvider[Topic])(rs: WrappedResultSet): Try[Topic] =
    fromResultSet(sp.resultName)(rs)

  def fromResultSet(rn: ResultName[Topic])(rs: WrappedResultSet): Try[Topic] = fromResultSet(rn.c _)(rs)

  def fromResultSet(colFunc: String => SQLSyntax)(rs: WrappedResultSet): Try[Topic] = Try {
    Topic(
      id = rs.long(colFunc("id")),
      title = rs.string(colFunc("title")),
      category_id = rs.long(colFunc("category_id")),
      created = NDLADate.fromUtcDate(rs.localDateTime(colFunc("created"))),
      updated = NDLADate.fromUtcDate(rs.localDateTime(colFunc("updated"))),
      ownerId = rs.longOpt(colFunc("owner_id")),
      deleted = rs.localDateTimeOpt(colFunc("deleted")).map(NDLADate.fromUtcDate),
      locked = rs.boolean(colFunc("locked")),
      pinned = rs.boolean(colFunc("pinned"))
    )
  }
}
