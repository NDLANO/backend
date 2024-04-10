/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.search.api.MultiSearchTermsAggregation
import sttp.tapir.Schema.annotations.description

@description("Search result for group search")
case class GroupSearchResult(
    @description("The total number of resources matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[MultiSearchSummary],
    @description("The suggestions for other searches") suggestions: Seq[MultiSearchSuggestion],
    @description("The aggregated fields if specified in query") aggregations: Seq[MultiSearchTermsAggregation],
    @description("Type of resources in this object") resourceType: String
)

object GroupSearchResult {
  implicit val encoder: Encoder[GroupSearchResult] = deriveEncoder
  implicit val decoder: Decoder[GroupSearchResult] = deriveDecoder
}
