/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

case class SearchableTaxonomyContext(
    id: String,
    subjectId: String,
    subject: SearchableLanguageValues,
    path: String,
    breadcrumbs: SearchableLanguageList,
    contextType: String,
    relevanceId: Option[String],
    relevance: SearchableLanguageValues,
    resourceTypes: List[SearchableTaxonomyResourceType],
    parentTopicIds: List[String]
)
