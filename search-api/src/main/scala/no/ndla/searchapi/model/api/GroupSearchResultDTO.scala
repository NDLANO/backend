/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.api.search.{MultiSearchSuggestionDTO, MultiSearchSummaryDTO, MultiSearchTermsAggregationDTO}
import sttp.tapir.Schema.annotations.description

@description("Search result for group search")
case class GroupSearchResultDTO(
    @description("The total number of resources matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[MultiSearchSummaryDTO],
    @description("The suggestions for other searches") suggestions: Seq[MultiSearchSuggestionDTO],
    @description("The aggregated fields if specified in query") aggregations: Seq[MultiSearchTermsAggregationDTO],
    @description("Type of resources in this object") resourceType: String
)

object GroupSearchResultDTO {
  implicit val encoder: Encoder[GroupSearchResultDTO] = deriveEncoder
  implicit val decoder: Decoder[GroupSearchResultDTO] = deriveDecoder
}
