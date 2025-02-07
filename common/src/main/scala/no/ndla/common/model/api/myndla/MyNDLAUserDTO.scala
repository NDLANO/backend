/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.myndla

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.myndla.ArenaGroup
import sttp.tapir.Schema.annotations.description

case class MyNDLAGroupDTO(
    @description("ID of the group") id: String,
    @description("Name of the group") displayName: String,
    @description("Is this the primary school") isPrimarySchool: Boolean,
    @description("ID of parent group") parentId: Option[String]
)

object MyNDLAGroupDTO {
  implicit def encoder: Encoder[MyNDLAGroupDTO] = deriveEncoder
  implicit def decoder: Decoder[MyNDLAGroupDTO] = deriveDecoder
}

case class MyNDLAUserDTO(
    @description("ID of the user") id: Long,
    @description("FeideID of the user") feideId: String,
    @description("Username of the user") username: String,
    @description("Email address of the user") email: String,
    @description("Name of the user") displayName: String,
    @description("Favorite subjects of the user") favoriteSubjects: Seq[String],
    @description("User role") role: String,
    @description("User root organization") organization: String,
    @description("User groups") groups: Seq[MyNDLAGroupDTO],
    @description("Whether arena is explicitly enabled for the user") arenaEnabled: Boolean,
    @description("Whether the user has accepted the use of arena") arenaAccepted: Boolean,
    @description("Arena user groups") arenaGroups: List[ArenaGroup]
)

object MyNDLAUserDTO {
  implicit def encoder: Encoder[MyNDLAUserDTO] = deriveEncoder
  implicit def decoder: Decoder[MyNDLAUserDTO] = deriveDecoder
}

case class UpdatedMyNDLAUserDTO(
    @description("Favorite subjects of the user")
    favoriteSubjects: Option[Seq[String]],
    @description("Whether arena should explicitly be enabled for the user")
    arenaEnabled: Option[Boolean],
    @description("Which arena groups the user should be in, only modifiable by admins")
    arenaGroups: Option[List[ArenaGroup]],
    @description("Whether the user has accepted the use of arena")
    arenaAccepted: Option[Boolean]
)

object UpdatedMyNDLAUserDTO {
  implicit def encoder: Encoder[UpdatedMyNDLAUserDTO] = deriveEncoder
  implicit def decoder: Decoder[UpdatedMyNDLAUserDTO] = deriveDecoder
}
