/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.WithLanguageAndValue
import sttp.tapir.Schema.annotations.description

case class DisclaimerDTO(
    @description("The freetext html content of the disclaimer") disclaimer: String,
    @description("ISO 639-1 code that represents the language used in the disclaimer") language: String
)

object DisclaimerDTO {
  def fromLanguageValue(lv: WithLanguageAndValue[String]): DisclaimerDTO = DisclaimerDTO(lv.value, lv.language)

  implicit def encoder: Encoder[DisclaimerDTO]          = deriveEncoder
  implicit def decoder: Decoder[DisclaimerDTO]          = deriveDecoder
  implicit def schema: sttp.tapir.Schema[DisclaimerDTO] = sttp.tapir.Schema.derived[DisclaimerDTO]
}
