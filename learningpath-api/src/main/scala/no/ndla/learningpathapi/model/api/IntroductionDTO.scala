/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField
import sttp.tapir.Schema.annotations.description

@description("The introduction of the learningpath")
case class IntroductionDTO(
    @description("The introduction to the learningpath. Basic HTML allowed")
    introduction: String,
    @description("ISO 639-1 code that represents the language used in introduction")
    language: String,
) extends LanguageField[String] {
  override def value: String    = introduction
  override def isEmpty: Boolean = introduction.isEmpty
}

object IntroductionDTO {
  implicit val encoder: Encoder[IntroductionDTO] = deriveEncoder
  implicit val decoder: Decoder[IntroductionDTO] = deriveDecoder
}
