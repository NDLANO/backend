/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Information about the responsible")
case class DraftResponsible(
    @(ApiModelProperty @field)(description = "NDLA ID of responsible editor") responsibleId: String,
    @(ApiModelProperty @field)(description = "Date of when the responsible editor was last updated") lastUpdated: NDLADate,
)

object DraftResponsible {
  implicit def encoder: Encoder[DraftResponsible] = deriveEncoder
  implicit def decoder: Decoder[DraftResponsible] = deriveDecoder
}
