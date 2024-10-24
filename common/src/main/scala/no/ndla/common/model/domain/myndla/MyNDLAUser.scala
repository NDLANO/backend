/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.myndla

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate

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
  def toFullUser(id: Long, feideId: String): MyNDLAUser = {
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

case class MyNDLAUser(
    id: Long,
    feideId: String,
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

object MyNDLAUser {
  implicit val encoder: Encoder[MyNDLAUser] = deriveEncoder
  implicit val decoder: Decoder[MyNDLAUser] = deriveDecoder
}
