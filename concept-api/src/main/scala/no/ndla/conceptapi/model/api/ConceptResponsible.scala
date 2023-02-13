/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import java.time.LocalDateTime
import scala.annotation.meta.field

@ApiModel(description = "Information about the responsible")
case class ConceptResponsible(
    // format: off
    @(ApiModelProperty @field)(description = "NDLA ID of responsible editor") responsibleId: String,
    @(ApiModelProperty @field)(description = "Date of when the responsible editor was last updated") lastUpdated: LocalDateTime,
    // format: on
)
