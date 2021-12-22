/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.api.{MultiSearchSuggestion, MultiSearchSummary}

case class SearchResult(totalCount: Long,
                        page: Option[Int],
                        pageSize: Int,
                        language: String,
                        results: Seq[MultiSearchSummary],
                        suggestions: Seq[MultiSearchSuggestion],
                        aggregations: Seq[TermAggregation],
                        scrollId: Option[String] = None)
