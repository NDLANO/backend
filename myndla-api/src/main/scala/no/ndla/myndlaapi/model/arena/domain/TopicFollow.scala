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

case class TopicFollow(
    id: Long,
    user_id: Long,
    topic_id: Long
)

object TopicFollow extends SQLSyntaxSupport[TopicFollow] {
  override val tableName = "topic_follows"

  def fromResultSet(sp: SyntaxProvider[TopicFollow])(rs: WrappedResultSet): Try[TopicFollow] =
    fromResultSet(sp.resultName)(rs)
  def fromResultSet(rn: ResultName[TopicFollow])(rs: WrappedResultSet): Try[TopicFollow] = fromResultSet(rn.c _)(rs)

  def fromResultSet(colFunc: String => SQLSyntax)(rs: WrappedResultSet): Try[TopicFollow] = Try {
    TopicFollow(
      id = rs.long(colFunc("id")),
      user_id = rs.long(colFunc("user_id")),
      topic_id = rs.long(colFunc("topic_id"))
    )
  }
}
