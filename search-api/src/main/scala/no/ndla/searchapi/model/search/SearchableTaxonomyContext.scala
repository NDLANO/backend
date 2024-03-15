/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

// NOTE: This will need to match `TaxonomyContextDTO` in `taxonomy-api`
case class SearchableTaxonomyContext(
    publicId: String,
    contextId: String,
    rootId: String,
    root: SearchableLanguageValues,
    path: String,
    breadcrumbs: SearchableLanguageList,
    contextType: String,
    relevanceId: String,
    relevance: SearchableLanguageValues,
    resourceTypes: List[SearchableTaxonomyResourceType],
    parentIds: List[String],
    isPrimary: Boolean,
    isActive: Boolean
)
