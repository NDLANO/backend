/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

// NOTE: This will need to match `TaxonomyContextDTO` in `taxonomy-api`
case class SearchableTaxonomyContext(
    publicId: String,
    contextId: String,
    rootId: String,
    root: SearchableLanguageValues,
    path: Option[String],
    breadcrumbs: SearchableLanguageList,
    contextType: String,
    relevanceId: String,
    relevance: SearchableLanguageValues,
    resourceTypes: List[SearchableTaxonomyResourceType],
    parentIds: List[String],
    isPrimary: Boolean,
    isActive: Boolean,
    url: Option[String]
)

object SearchableTaxonomyContext {
  implicit val encoder: Encoder[SearchableTaxonomyContext] = deriveEncoder
  implicit val decoder: Decoder[SearchableTaxonomyContext] = deriveDecoder
}
