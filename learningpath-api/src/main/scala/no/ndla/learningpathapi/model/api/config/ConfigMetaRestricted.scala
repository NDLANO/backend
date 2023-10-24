/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.api.config

import org.scalatra.swagger.annotations.ApiModelProperty
import org.scalatra.swagger.runtime.annotations.ApiModel

import scala.annotation.meta.field

@ApiModel(description = "Describes configuration value.")
case class ConfigMetaRestricted(
    @(ApiModelProperty @field)(description = "Configuration key") key: String,
    @(ApiModelProperty @field)(description = "Configuration value.") value: Either[Boolean, List[String]]
)
