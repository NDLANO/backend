/*
 * Part of NDLA search-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import java.time.LocalDateTime
import scala.annotation.meta.field

// format: off
@ApiModel(description = "Information about the responsible")
case class DraftResponsible(
  @(ApiModelProperty @field)(description = "NDLA ID of responsible editor") responsibleId: String,
  @(ApiModelProperty @field)(description = "Date of when the responsible editor was last updated") lastUpdated: LocalDateTime,
)
