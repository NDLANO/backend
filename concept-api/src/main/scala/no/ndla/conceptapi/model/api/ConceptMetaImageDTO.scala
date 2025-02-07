/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.api
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Meta image for the concept")
case class ConceptMetaImageDTO(
    @description("The meta image url") url: String,
    @description("The alt text for the meta image") alt: String,
    @description(
      "The ISO 639-1 language code describing which concept translation this meta image belongs to"
    ) language: String
)

object ConceptMetaImageDTO {
  implicit val encoder: Encoder[ConceptMetaImageDTO] = deriveEncoder
  implicit val decoder: Decoder[ConceptMetaImageDTO] = deriveDecoder
}
