/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.myndla

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate

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
    arenaAccepted: Boolean,
    arenaGroups: List[ArenaGroup],
    shareNameAccepted: Boolean
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
      arenaAccepted = arenaAccepted,
      arenaGroups = arenaGroups,
      shareNameAccepted = shareNameAccepted
    )
  }
}

object MyNDLAUserDocument {
  implicit val encoder: Encoder[MyNDLAUserDocument] = deriveEncoder
  implicit val decoder: Decoder[MyNDLAUserDocument] = deriveDecoder
}
