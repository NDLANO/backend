/*
 * Part of NDLA concept-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Information about the responsible")
case class ConceptResponsibleDTO(
    @description("NDLA ID of responsible editor") responsibleId: String,
    @description("Date of when the responsible editor was last updated") lastUpdated: NDLADate
)

object ConceptResponsibleDTO {
  implicit val encoder: Encoder[ConceptResponsibleDTO] = deriveEncoder
  implicit val decoder: Decoder[ConceptResponsibleDTO] = deriveDecoder
}
