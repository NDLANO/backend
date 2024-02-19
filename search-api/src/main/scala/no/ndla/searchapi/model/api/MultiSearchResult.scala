/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.search.api.MultiSearchTermsAggregation
import sttp.tapir.Schema.annotations.description

@description("Information about search-results")
case class MultiSearchResult(
    @description("The total number of resources matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[MultiSearchSummary],
    @description("The suggestions for other searches") suggestions: Seq[MultiSearchSuggestion],
    @description("The aggregated fields if specified in query") aggregations: Seq[MultiSearchTermsAggregation]
)

object MultiSearchResult {
  implicit val encoder: Encoder[MultiSearchResult] = deriveEncoder
  implicit val decoder: Decoder[MultiSearchResult] = deriveDecoder
}
