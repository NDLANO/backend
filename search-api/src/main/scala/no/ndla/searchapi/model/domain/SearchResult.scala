/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.domain

import no.ndla.common.model.api.search.{MultiSearchSuggestionDTO, MultiSummaryBaseDTO}
import no.ndla.search.model.domain.TermAggregation

case class SearchResult(
    totalCount: Long,
    page: Option[Int],
    pageSize: Int,
    language: String,
    results: Seq[MultiSummaryBaseDTO],
    suggestions: Seq[MultiSearchSuggestionDTO],
    aggregations: Seq[TermAggregation],
    scrollId: Option[String] = None
)
