/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.runtime.annotations.ApiModelProperty
import scala.annotation.meta.field

case class MyNDLAUser(
    @(ApiModelProperty @field)(description = "ID of the user") id: Long,
    @(ApiModelProperty @field)(description = "Favorite subjects of the user") favoriteSubjects: Seq[String],
    @(ApiModelProperty @field)(description = "User role") role: String,
    @(ApiModelProperty @field)(description = "User organization") organization: String,
    @(ApiModelProperty @field)(description = "Whether arena is explicitly enabled for the user") arenaEnabled: Boolean,
    @(ApiModelProperty @field)(description = "Whether users name is shared with folders or not") shareName: Boolean
)

// format: off
case class UpdatedMyNDLAUser(
    @(ApiModelProperty @field)(description = "Favorite subjects of the user") favoriteSubjects: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "Whether arena should explicitly be enabled for the user") arenaEnabled: Option[Boolean],
    @(ApiModelProperty @field)(description = "Whether users name should be shared with folder or not") shareName: Option[Boolean]
)
