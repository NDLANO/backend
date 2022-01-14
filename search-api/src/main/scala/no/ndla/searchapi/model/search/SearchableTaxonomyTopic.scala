/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.search.model.SearchableLanguageValues

case class SearchableTaxonomyTopic(
    id: String,
    name: SearchableLanguageValues
)
