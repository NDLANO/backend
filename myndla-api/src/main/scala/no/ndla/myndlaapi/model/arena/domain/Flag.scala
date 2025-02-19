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

case class Flag(
    id: Long,
    user_id: Option[Long],
    post_id: Long,
    reason: String,
    created: NDLADate,
    resolved: Option[NDLADate]
) extends Owned {
  override def ownerId: Option[Long] = user_id
}

object Flag extends SQLSyntaxSupport[Flag] {
  override val tableName = "flags"

  def fromResultSet(sp: SyntaxProvider[Flag])(rs: WrappedResultSet): Try[Flag] = fromResultSet(sp.resultName)(rs)
  def fromResultSet(rn: ResultName[Flag])(rs: WrappedResultSet): Try[Flag]     = fromResultSet(rn.c _)(rs)

  def fromResultSet(colFunc: String => SQLSyntax)(rs: WrappedResultSet): Try[Flag] = Try {
    Flag(
      id = rs.long(colFunc("id")),
      user_id = rs.longOpt(colFunc("user_id")),
      post_id = rs.long(colFunc("post_id")),
      reason = rs.string(colFunc("reason")),
      created = NDLADate.fromUtcDate(rs.localDateTime(colFunc("created"))),
      resolved = rs.localDateTimeOpt(colFunc("resolved")).map(NDLADate.fromUtcDate)
    )
  }
}
