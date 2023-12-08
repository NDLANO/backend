/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
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
    @(ApiModelProperty @field)(description = "ID of the user") id: Long,
    @(ApiModelProperty @field)(description = "FeideID of the user") feideId: String,
    @(ApiModelProperty @field)(description = "Username of the user") username: String,
    @(ApiModelProperty @field)(description = "Email address of the user") email: String,
    @(ApiModelProperty @field)(description = "Name of the user") displayName: String,
    @(ApiModelProperty @field)(description = "Favorite subjects of the user") favoriteSubjects: Seq[String],
    @(ApiModelProperty @field)(description = "User role") role: String,
    @(ApiModelProperty @field)(description = "User root organization") organization: String,
    @(ApiModelProperty @field)(description = "User groups") groups: Seq[MyNDLAGroup],
    @(ApiModelProperty @field)(description = "Whether arena is explicitly enabled for the user") arenaEnabled: Boolean,
    @(ApiModelProperty @field)(description = "Whether users name is shared with folders or not") shareName: Boolean
)

object MyNDLAUser {
  implicit def encoder: Encoder[MyNDLAUser] = deriveEncoder
  implicit def decoder: Decoder[MyNDLAUser] = deriveDecoder
}

// format: off
case class UpdatedMyNDLAUser(
    @(ApiModelProperty @field)(description = "Favorite subjects of the user") favoriteSubjects: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "Whether arena should explicitly be enabled for the user") arenaEnabled: Option[Boolean],
    @(ApiModelProperty @field)(description = "Whether users name should be shared with folder or not") shareName: Option[Boolean]
)

object UpdatedMyNDLAUser {
  implicit def encoder: Encoder[UpdatedMyNDLAUser] = deriveEncoder
  implicit def decoder: Decoder[UpdatedMyNDLAUser] = deriveDecoder
}
