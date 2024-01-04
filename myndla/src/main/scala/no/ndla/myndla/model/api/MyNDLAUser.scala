/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.myndla.model.domain.ArenaGroup
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

case class MyNDLAGroup(
    @(ApiModelProperty @field)(description = "ID of the group") id: String,
    @(ApiModelProperty @field)(description = "Name of the group") displayName: String,
    @(ApiModelProperty @field)(description = "Is this the primary school") isPrimarySchool: Boolean,
    @(ApiModelProperty @field)(description = "ID of parent group") parentId: Option[String]
)

object MyNDLAGroup {
  implicit def encoder: Encoder[MyNDLAGroup] = deriveEncoder
  implicit def decoder: Decoder[MyNDLAGroup] = deriveDecoder
}

case class MyNDLAUser(
    @ApiModelProperty(description = "ID of the user") id: Long,
    @ApiModelProperty(description = "FeideID of the user") feideId: String,
    @ApiModelProperty(description = "Username of the user") username: String,
    @ApiModelProperty(description = "Email address of the user") email: String,
    @ApiModelProperty(description = "Name of the user") displayName: String,
    @ApiModelProperty(description = "Favorite subjects of the user") favoriteSubjects: Seq[String],
    @ApiModelProperty(description = "User role") role: String,
    @ApiModelProperty(description = "User root organization") organization: String,
    @ApiModelProperty(description = "User groups") groups: Seq[MyNDLAGroup],
    @ApiModelProperty(description = "Whether arena is explicitly enabled for the user") arenaEnabled: Boolean,
    @ApiModelProperty(description = "Whether users name is shared with folders or not") shareName: Boolean,
    @ApiModelProperty(description = "Arena user groups") arenaGroups: List[ArenaGroup]
)

object MyNDLAUser {
  implicit def encoder: Encoder[MyNDLAUser] = deriveEncoder
  implicit def decoder: Decoder[MyNDLAUser] = deriveDecoder
}

// format: off
case class UpdatedMyNDLAUser(
    @(ApiModelProperty @field)(description = "Favorite subjects of the user") favoriteSubjects: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "Whether arena should explicitly be enabled for the user") arenaEnabled: Option[Boolean],
    @(ApiModelProperty @field)(description = "Whether users name should be shared with folder or not") shareName: Option[Boolean],
    @(ApiModelProperty @field)(description = "Which arena groups the user should be in, only modifiable by admins") arenaGroups: Option[List[ArenaGroup]]
)

object UpdatedMyNDLAUser {
  implicit def encoder: Encoder[UpdatedMyNDLAUser] = deriveEncoder
  implicit def decoder: Decoder[UpdatedMyNDLAUser] = deriveDecoder
}
