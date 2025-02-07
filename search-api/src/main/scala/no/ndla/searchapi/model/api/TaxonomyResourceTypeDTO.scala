/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

@description("Taxonomy resource type")
case class TaxonomyResourceTypeDTO(
    @description("Id of the taoxonomy resource type") id: String,
    @description("Name of the taoxonomy resource type") name: String,
    @description("The ISO 639-1 language code for the resource type") language: String
)

object TaxonomyResourceTypeDTO {
  implicit val encoder: Encoder[TaxonomyResourceTypeDTO] = deriveEncoder
  implicit val decoder: Decoder[TaxonomyResourceTypeDTO] = deriveDecoder
}
