/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

case class DisclaimerDTO(
    @description("The freetext content of the disclaimer") disclaimer: String,
    @description("The freetext html content of the disclaimer") htmlDisclaimer: String,
    @description("ISO 639-1 code that represents the language used in the disclaimer") language: String
)

object DisclaimerDTO {
  implicit def encoder: Encoder[DisclaimerDTO] = deriveEncoder
  implicit def decoder: Decoder[DisclaimerDTO] = deriveDecoder
}
