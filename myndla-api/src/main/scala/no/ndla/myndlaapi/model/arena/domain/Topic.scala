/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.domain

import no.ndla.common.model.NDLADate
import scalikejdbc._

import scala.util.Try

case class Topic(
    id: Long,
    ownerId: Int,
    title: String,
    category_id: Long,
    created: NDLADate,
    updated: NDLADate
)

object Topic extends SQLSyntaxSupport[Topic] {
  override val tableName: String = "topics"

  def fromResultSet(sp: SyntaxProvider[Topic])(rs: WrappedResultSet): Try[Topic] =
    fromResultSet(sp.resultName)(rs)

  def fromResultSet(rn: ResultName[Topic])(rs: WrappedResultSet): Try[Topic] = Try {
    Topic(
      id = rs.long(rn.c("id")),
      title = rs.string(rn.c("title")),
      category_id = rs.long(rn.c("category_id")),
      created = NDLADate.fromUtcDate(rs.localDateTime(rn.c("created"))),
      updated = NDLADate.fromUtcDate(rs.localDateTime(rn.c("updated"))),
      ownerId = rs.int(rn.c("owner_id"))
    )
  }
}
