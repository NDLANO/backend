/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.search.model.SearchableLanguageValues

case class SearchableTaxonomyResourceType(
    id: String,
    name: SearchableLanguageValues
)

object SearchableTaxonomyResourceType {
  implicit val encoder: Encoder[SearchableTaxonomyResourceType] = deriveEncoder
  implicit val decoder: Decoder[SearchableTaxonomyResourceType] = deriveDecoder
}
