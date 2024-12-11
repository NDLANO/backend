/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.search.api.MultiSearchTermsAggregationDTO
import sttp.tapir.Schema.annotations.description

@description("Information about search-results")
case class MultiSearchResultDTO(
    @description("The total number of resources matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[MultiSearchSummaryDTO],
    @description("The suggestions for other searches") suggestions: Seq[MultiSearchSuggestionDTO],
    @description("The aggregated fields if specified in query") aggregations: Seq[MultiSearchTermsAggregationDTO]
)

object MultiSearchResultDTO {
  implicit val encoder: Encoder[MultiSearchResultDTO] = deriveEncoder
  implicit val decoder: Decoder[MultiSearchResultDTO] = deriveDecoder
}