/*
 * Part of NDLA myndla-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import com.scalatsi.TypescriptType.{TSLiteralString, TSUnion}
import com.scalatsi.{TSNamedType, TSType}
import enumeratum.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.network.model.FeideID
import scalikejdbc.*

case class MyNDLAGroup(id: String, displayName: String, isPrimarySchool: Boolean, parentId: Option[String])
object MyNDLAGroup {
  implicit val encoder: Encoder[MyNDLAGroup] = deriveEncoder
  implicit val decoder: Decoder[MyNDLAGroup] = deriveDecoder
}

case class MyNDLAUserDocument(
    favoriteSubjects: Seq[String],
    userRole: UserRole.Value,
    lastUpdated: NDLADate,
    organization: String,
    groups: Seq[MyNDLAGroup],
    username: String,
    displayName: String,
    email: String,
    arenaEnabled: Boolean,
    arenaGroups: List[ArenaGroup],
    shareName: Boolean
) {
  def toFullUser(id: Long, feideId: FeideID): MyNDLAUser = {
    MyNDLAUser(
      id = id,
      feideId = feideId,
      favoriteSubjects = favoriteSubjects,
      userRole = userRole,
      lastUpdated = lastUpdated,
      organization = organization,
      groups = groups,
      username = username,
      displayName = displayName,
      email = email,
      arenaEnabled = arenaEnabled,
      shareName = shareName,
      arenaGroups = arenaGroups
    )
  }
}

object MyNDLAUserDocument {
  implicit val encoder: Encoder[MyNDLAUserDocument] = deriveEncoder
  implicit val decoder: Decoder[MyNDLAUserDocument] = deriveDecoder
}

sealed trait ArenaGroup extends EnumEntry
object ArenaGroup extends Enum[ArenaGroup] with CirceEnum[ArenaGroup] {
  case object ADMIN extends ArenaGroup
  override def values: IndexedSeq[ArenaGroup] = findValues

  implicit val enumTsType: TSNamedType[ArenaGroup] =
    TSType.alias[ArenaGroup]("ArenaGroup", TSUnion(values.map(e => TSLiteralString(e.entryName))))

}

case class MyNDLAUser(
    id: Long,
    feideId: FeideID,
    favoriteSubjects: Seq[String],
    userRole: UserRole.Value,
    lastUpdated: NDLADate,
    organization: String,
    groups: Seq[MyNDLAGroup],
    username: String,
    displayName: String,
    email: String,
    arenaEnabled: Boolean,
    arenaGroups: List[ArenaGroup],
    shareName: Boolean
) {
  // Keeping FEIDE and our data in sync
  def wasUpdatedLast24h: Boolean = NDLADate.now().isBefore(lastUpdated.minusSeconds(10))

  def isStudent: Boolean = userRole == UserRole.STUDENT
  def isTeacher: Boolean = userRole == UserRole.EMPLOYEE
  def isAdmin: Boolean   = arenaGroups.contains(ArenaGroup.ADMIN)
}

object MyNDLAUser extends SQLSyntaxSupport[MyNDLAUser] {
  implicit val encoder: Encoder[MyNDLAUser] = deriveEncoder
  implicit val decoder: Decoder[MyNDLAUser] = deriveDecoder

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
