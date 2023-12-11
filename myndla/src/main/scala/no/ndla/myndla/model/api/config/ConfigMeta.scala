/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api.config

import no.ndla.common.model.NDLADate
import org.scalatra.swagger.annotations.ApiModelProperty
import org.scalatra.swagger.runtime.annotations.ApiModel

@ApiModel(description = "Describes configuration value.")
case class ConfigMeta(
    @ApiModelProperty(description = "Configuration key") key: String,
    @ApiModelProperty(description = "Configuration value.") value: Either[Boolean, List[String]],
    @ApiModelProperty(description = "Date of when configuration was last updated") updatedAt: NDLADate,
    @ApiModelProperty(description = "UserId of who last updated the configuration parameter.") updatedBy: String
)