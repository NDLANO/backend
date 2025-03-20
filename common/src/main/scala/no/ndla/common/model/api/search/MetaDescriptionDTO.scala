/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.WithLanguage
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

@description("Meta description of the resource")
case class MetaDescriptionDTO(
    @description("The meta description") metaDescription: String,
    @description(
      "The ISO 639-1 language code describing which article translation this meta description belongs to"
    ) language: String
) extends WithLanguage

object MetaDescriptionDTO {
  implicit val encoder: Encoder[MetaDescriptionDTO] = deriveEncoder
  implicit val decoder: Decoder[MetaDescriptionDTO] = deriveDecoder
  implicit val schema: Schema[MetaDescriptionDTO]   = Schema.derived
}
