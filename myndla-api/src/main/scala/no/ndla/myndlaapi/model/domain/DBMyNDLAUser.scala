/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.myndla.{MyNDLAUser, MyNDLAUserDocument}
import scalikejdbc.*

object DBMyNDLAUser extends SQLSyntaxSupport[MyNDLAUser] {

  override val tableName = "my_ndla_users"

  def fromResultSet(lp: SyntaxProvider[MyNDLAUser])(rs: WrappedResultSet): MyNDLAUser =
    fromResultSet((s: String) => lp.resultName.c(s))(rs)

  def fromResultSet(rs: WrappedResultSet): MyNDLAUser = fromResultSet((s: String) => s)(rs)

  def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): MyNDLAUser = {
    val jsonString = rs.string(colNameWrapper("document"))
    val metaData   = CirceUtil.unsafeParseAs[MyNDLAUserDocument](jsonString)
    val id         = rs.long(colNameWrapper("id"))
    val feideId    = rs.string(colNameWrapper("feide_id"))

    metaData.toFullUser(
      id = id,
      feideId = feideId
    )
  }
}
