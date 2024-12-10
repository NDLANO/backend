/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain
import no.ndla.search.model.domain.TermAggregation
import no.ndla.searchapi.model.api.{MultiSearchSuggestionDTO, MultiSearchSummaryDTO}

case class SearchResult(
    totalCount: Long,
    page: Option[Int],
    pageSize: Int,
    language: String,
    results: Seq[MultiSearchSummaryDTO],
    suggestions: Seq[MultiSearchSuggestionDTO],
    aggregations: Seq[TermAggregation],
    scrollId: Option[String] = None
)
