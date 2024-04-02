/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.search.api.MultiSearchTermsAggregation
import sttp.tapir.Schema.annotations.description

@description("Information about search-results")
case class ConceptSearchResult(
    @description("The total number of articles matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[ConceptSummary],
    @description("The aggregated fields if specified in query") aggregations: Seq[
      MultiSearchTermsAggregation
    ]
)

object ConceptSearchResult {
  implicit val encoder: Encoder[ConceptSearchResult] = deriveEncoder
  implicit val decoder: Decoder[ConceptSearchResult] = deriveDecoder
}
