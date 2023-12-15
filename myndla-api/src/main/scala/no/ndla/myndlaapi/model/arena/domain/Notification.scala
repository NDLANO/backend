/*
 * Part of NDLA myndla-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.arena.domain

import no.ndla.common.model.NDLADate
import scalikejdbc._

import scala.util.Try

case class Notification(
    id: Long,
    user_id: Long,
    post_id: Long,
    topic_id: Long,
    is_read: Boolean,
    notification_time: NDLADate
)

object Notification extends SQLSyntaxSupport[Notification] {
  override val tableName: String = "notifications"

  def fromResultSet(sp: SyntaxProvider[Notification])(rs: WrappedResultSet): Try[Notification] =
    fromResultSet(sp.resultName)(rs)

  def fromResultSet(rn: ResultName[Notification])(rs: WrappedResultSet): Try[Notification] =
    fromResultSet(rn.c _)(rs)

  def fromResultSet(colFunc: String => SQLSyntax)(rs: WrappedResultSet): Try[Notification] = Try {
    Notification(
      id = rs.long(colFunc("id")),
      user_id = rs.long(colFunc("user_id")),
      post_id = rs.long(colFunc("post_id")),
      topic_id = rs.long(colFunc("topic_id")),
      is_read = rs.boolean(colFunc("is_read")),
      notification_time = NDLADate.fromUtcDate(rs.localDateTime(colFunc("notification_time")))
    )
  }
}
