/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.myndla.{MyNDLAUser, MyNDLAUserDocument}
import no.ndla.myndlaapi.Props
import scalikejdbc.*

class DBMyNDLAUser(using props: Props) extends SQLSyntaxSupport[MyNDLAUser] {

  override val tableName                  = "my_ndla_users"
  override val schemaName: Option[String] = Some(props.MetaSchema)

  def fromResultSet(lp: SyntaxProvider[MyNDLAUser])(rs: WrappedResultSet): MyNDLAUser =
    fromResultSetWithWrapper((s: String) => lp.resultName.c(s))(rs)

  private def fromResultSetWithWrapper(colNameWrapper: String => String)(rs: WrappedResultSet): MyNDLAUser = {
    val jsonString = rs.string(colNameWrapper("document"))
    val metaData   = CirceUtil.unsafeParseAs[MyNDLAUserDocument](jsonString)
    val id         = rs.long(colNameWrapper("id"))
    val feideId    = rs.string(colNameWrapper("feide_id"))
    val lastSeen   = NDLADate.fromUtcDate(rs.localDateTime(colNameWrapper("last_seen")))

    metaData.toFullUser(id = id, feideId = feideId, lastSeen = lastSeen)
  }
}
