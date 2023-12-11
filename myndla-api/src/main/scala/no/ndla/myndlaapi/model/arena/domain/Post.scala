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

case class Post(
    id: Long,
    content: String,
    topic_id: Long,
    created: NDLADate,
    updated: NDLADate,
    ownerId: Long
)

object Post extends SQLSyntaxSupport[Post] {
  override val tableName: String = "posts"

  def fromResultSet(sp: SyntaxProvider[Post])(rs: WrappedResultSet): Try[Post] =
    fromResultSet(sp.resultName)(rs)

  def fromResultSet(rn: ResultName[Post])(rs: WrappedResultSet): Try[Post] = Try {
    Post(
      id = rs.long(rn.c("id")),
      content = rs.string(rn.c("content")),
      topic_id = rs.long(rn.c("topic_id")),
      created = NDLADate.fromUtcDate(rs.localDateTime(rn.c("created"))),
      updated = NDLADate.fromUtcDate(rs.localDateTime(rn.c("updated"))),
      ownerId = rs.int(rn.c("owner_id"))
    )
  }
}
