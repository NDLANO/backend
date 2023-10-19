/*
 * Part of NDLA draft-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

// format: off
@ApiModel(description = "A status in the status state machine")
case class StateMachineStatus(
    @ApiModelProperty(description = "Name of the status") name: String,
    @ApiModelProperty(description = "Whether the status is enabled from the status of the parent in the object") enabled: Boolean
)
