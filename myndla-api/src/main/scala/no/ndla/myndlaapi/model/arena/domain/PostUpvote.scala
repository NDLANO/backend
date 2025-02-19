/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.arena.domain

import scalikejdbc._

import scala.util.Try

case class PostUpvote(
    id: Long,
    user_id: Long,
    post_id: Long
)

object PostUpvote extends SQLSyntaxSupport[PostUpvote] {
  override val tableName = "post_upvote"

  def fromResultSet(sp: SyntaxProvider[PostUpvote])(rs: WrappedResultSet): Try[PostUpvote] =
    fromResultSet(sp.resultName)(rs)
  def fromResultSet(rn: ResultName[PostUpvote])(rs: WrappedResultSet): Try[PostUpvote] = fromResultSet(rn.c _)(rs)

  def fromResultSet(colFunc: String => SQLSyntax)(rs: WrappedResultSet): Try[PostUpvote] = Try {
    PostUpvote(
      id = rs.long(colFunc("id")),
      user_id = rs.long(colFunc("user_id")),
      post_id = rs.long(colFunc("post_id"))
    )
  }
}
