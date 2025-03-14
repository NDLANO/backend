/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.ListDTO
import sttp.tapir.Schema.annotations.description

@description("Information about search-results")
case class MultiSearchResultDTO(
    @description("The total number of resources matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: ListDTO[MultiSummaryBaseDTO],
    @description("The suggestions for other searches") suggestions: ListDTO[MultiSearchSuggestionDTO],
    @description("The aggregated fields if specified in query") aggregations: ListDTO[MultiSearchTermsAggregationDTO]
)

object MultiSearchResultDTO {
  implicit val encoder: Encoder[MultiSearchResultDTO] = deriveEncoder
  implicit val decoder: Decoder[MultiSearchResultDTO] = deriveDecoder
}
