/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations.ApiModelProperty
import scala.annotation.meta.field

case class ExportedUserData(
    @(ApiModelProperty @field)(description = "The users data") userData: FeideUser,
    @(ApiModelProperty @field)(description = "The users folders") folders: List[Folder]
)
