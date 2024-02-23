/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.WithLanguage
import sttp.tapir.Schema.annotations.description

@description("Meta description of the resource")
case class MetaDescription(
    @description("The meta description") metaDescription: String,
    @description(
      "The ISO 639-1 language code describing which article translation this meta description belongs to"
    ) language: String
) extends WithLanguage

object MetaDescription {
  implicit val encoder: Encoder[MetaDescription] = deriveEncoder
  implicit val decoder: Decoder[MetaDescription] = deriveDecoder
}
