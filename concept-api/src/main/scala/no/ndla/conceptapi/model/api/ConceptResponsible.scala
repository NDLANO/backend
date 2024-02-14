/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field
import no.ndla.common.model.NDLADate

@ApiModel(description = "Information about the responsible")
case class ConceptResponsible(
    // format: off
    @(ApiModelProperty @field)(description = "NDLA ID of responsible editor") responsibleId: String,
    @(ApiModelProperty @field)(description = "Date of when the responsible editor was last updated") lastUpdated: NDLADate,
    // format: on
)

object ConceptResponsible {
  implicit val encoder: Encoder[ConceptResponsible] = deriveEncoder
  implicit val decoder: Decoder[ConceptResponsible] = deriveDecoder
}
