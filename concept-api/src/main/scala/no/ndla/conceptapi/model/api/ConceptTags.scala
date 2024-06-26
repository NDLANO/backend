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

@description("Meta image for the concept")
case class ConceptTags(
    @description("Searchable tags") tags: Seq[String],
    @description(
      "The ISO 639-1 language code describing which concept translation these tags belongs to"
    ) language: String
)

object ConceptTags {
  implicit val encoder: Encoder[ConceptTags] = deriveEncoder
  implicit val decoder: Decoder[ConceptTags] = deriveDecoder
}
