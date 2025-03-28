/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Description of a visual element")
case class VisualElementDTO(
    @description(
      "Html containing the visual element. May contain any legal html element, including the embed-tag"
    ) visualElement: String,
    @description(
      "The ISO 639-1 language code describing which article translation this visual element belongs to"
    ) language: String
)

object VisualElementDTO {
  implicit def encoder: Encoder[VisualElementDTO] = deriveEncoder
  implicit def decoder: Decoder[VisualElementDTO] = deriveDecoder
}
