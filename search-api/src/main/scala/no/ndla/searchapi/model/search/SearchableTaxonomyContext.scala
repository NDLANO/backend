/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.search.SearchableLanguageList
import no.ndla.common.model.taxonomy.TaxonomyContext

case class SearchableTaxonomyContext(
    domainObject: TaxonomyContext,
    publicId: String,
    contextId: String,
    rootId: String,
    path: String,
    breadcrumbs: SearchableLanguageList,
    contextType: String,
    relevanceId: String,
    resourceTypeIds: List[String],
    parentIds: List[String],
    isPrimary: Boolean,
    isActive: Boolean,
    isVisible: Boolean,
    url: String
)

object SearchableTaxonomyContext {
  implicit val encoder: Encoder[SearchableTaxonomyContext] = deriveEncoder
  implicit val decoder: Decoder[SearchableTaxonomyContext] = deriveDecoder
}
