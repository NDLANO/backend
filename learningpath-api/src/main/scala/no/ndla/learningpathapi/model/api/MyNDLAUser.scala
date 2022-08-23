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
    @(ApiModelProperty @field)(description = "Favorite subjects of the user") favoriteSubjects: Seq[String]
)

case class UpdatedMyNDLAUser(
    @(ApiModelProperty @field)(description = "Favorite subjects of the user") favoriteSubjects: Option[Seq[String]]
)
