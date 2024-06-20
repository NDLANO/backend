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
    ownerId: Option[Long],
    toPostId: Option[Long]
) extends Owned

object Post extends SQLSyntaxSupport[Post] {
  override val tableName: String = "posts"

  def fromResultSet(sp: SyntaxProvider[Post])(rs: WrappedResultSet): Try[Post] =
    fromResultSet(sp.resultName)(rs)

  def fromResultSet(rn: ResultName[Post])(rs: WrappedResultSet): Try[Post] =
    fromResultSet(rn.c _)(rs)

  def fromResultSet(colFunc: String => SQLSyntax)(rs: WrappedResultSet): Try[Post] = Try {
    Post(
      id = rs.long(colFunc("id")),
      content = rs.string(colFunc("content")),
      topic_id = rs.long(colFunc("topic_id")),
      created = NDLADate.fromUtcDate(rs.localDateTime(colFunc("created"))),
      updated = NDLADate.fromUtcDate(rs.localDateTime(colFunc("updated"))),
      ownerId = rs.longOpt(colFunc("owner_id")),
      toPostId = rs.longOpt(colFunc("to_post_id"))
    )
  }
}
