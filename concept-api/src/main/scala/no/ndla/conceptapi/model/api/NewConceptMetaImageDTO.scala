/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

case class NewConceptMetaImageDTO(
    @description("The image-api id of the meta image") id: String,
    @description("The alt text of the meta image") alt: String
)

object NewConceptMetaImageDTO {
  implicit val encoder: Encoder[NewConceptMetaImageDTO] = deriveEncoder
  implicit val decoder: Decoder[NewConceptMetaImageDTO] = deriveDecoder
}
