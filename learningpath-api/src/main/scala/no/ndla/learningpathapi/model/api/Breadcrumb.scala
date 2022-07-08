/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import scala.annotation.meta.field
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

case class Breadcrumb(
    @(ApiModelProperty @field)(description = "UUID of the folder") id: String,
    @(ApiModelProperty @field)(description = "Folder name") name: String
)
