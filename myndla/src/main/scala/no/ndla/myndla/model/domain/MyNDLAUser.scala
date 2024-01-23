/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain

import com.scalatsi.TypescriptType.{TSLiteralString, TSUnion}
import com.scalatsi.{TSNamedType, TSType}
import enumeratum._
import no.ndla.common.model.NDLADate
import no.ndla.network.model.FeideID
import org.json4s.FieldSerializer._
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import scalikejdbc._

case class MyNDLAGroup(id: String, displayName: String, isPrimarySchool: Boolean, parentId: Option[String])
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

object DBMyNDLAUser extends SQLSyntaxSupport[MyNDLAUser] {
  implicit val jsonEncoder: Formats =
    DefaultFormats +
      new EnumNameSerializer(UserRole) ++
      JavaTimeSerializers.all +
      NDLADate.Json4sSerializer +
      Json4s.serializer(ArenaGroup)

  override val tableName = "my_ndla_users"

  val repositorySerializer: Formats = jsonEncoder + FieldSerializer[MyNDLAUser](
    ignore("id") orElse
      ignore("feideId")
  )

  def fromResultSet(lp: SyntaxProvider[MyNDLAUser])(rs: WrappedResultSet): MyNDLAUser =
    fromResultSet((s: String) => lp.resultName.c(s))(rs)

  def fromResultSet(rs: WrappedResultSet): MyNDLAUser = fromResultSet((s: String) => s)(rs)

  def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): MyNDLAUser = {
    val jsonString = rs.string(colNameWrapper("document"))
    val metaData   = read[MyNDLAUserDocument](jsonString)
    val id         = rs.long(colNameWrapper("id"))
    val feideId    = rs.string(colNameWrapper("feide_id"))

    metaData.toFullUser(
      id = id,
      feideId = feideId
    )
  }
}
