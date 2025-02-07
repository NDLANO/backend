/*
 * Part of NDLA search
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Value that appears in the search aggregation")
case class TermValueDTO(
    @description("Value that appeared in result") value: String,
    @description("Number of times the value appeared in result") count: Int
)

object TermValueDTO {
  implicit val encoder: Encoder[TermValueDTO] = deriveEncoder
  implicit val decoder: Decoder[TermValueDTO] = deriveDecoder
}

// format: off
@description("Information about search aggregation on `field`")
case class MultiSearchTermsAggregationDTO(
    @description("The field the specific aggregation is matching")
    field: String,
    @description("Number of documents with values that didn't appear in the aggregation. (Will only happen if there are more than 50 different values)")
    sumOtherDocCount: Int,
    @description("The result is approximate, this gives an approximation of potential errors. (Specifics here: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html#search-aggregations-bucket-terms-aggregation-approximate-counts)")
    docCountErrorUpperBound: Int,
    @description("Values appearing in the field") values: Seq[TermValueDTO]
)
// format: on

object MultiSearchTermsAggregationDTO {
  implicit val encoder: Encoder[MultiSearchTermsAggregationDTO] = deriveEncoder
  implicit val decoder: Decoder[MultiSearchTermsAggregationDTO] = deriveDecoder
}
