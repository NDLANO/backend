/*
 * Part of NDLA search
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.search.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Value that appears in the search aggregation")
case class TermValue(
    @(ApiModelProperty @field)(description = "Value that appeared in result") value: String,
    @(ApiModelProperty @field)(description = "Number of times the value appeared in result") count: Int
)

object TermValue {
  implicit val encoder: Encoder[TermValue] = deriveEncoder
  implicit val decoder: Decoder[TermValue] = deriveDecoder
}

@ApiModel(description = "Information about search aggregation on `field`")
case class MultiSearchTermsAggregation(
    @(ApiModelProperty @field)(description = "The field the specific aggregation is matching") field: String,
    @(ApiModelProperty @field)(description = "Number of documents with values that didn't appear in the aggregation. (Will only happen if there are more than 50 different values)") sumOtherDocCount: Int,
    @(ApiModelProperty @field)(description = "The result is approximate, this gives an approximation of potential errors. (Specifics here: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html#search-aggregations-bucket-terms-aggregation-approximate-counts)")
    docCountErrorUpperBound: Int,
    @(ApiModelProperty @field)(description = "Values appearing in the field") values: Seq[TermValue]
)
// format: on

object MultiSearchTermsAggregation {
  implicit val encoder: Encoder[MultiSearchTermsAggregation] = deriveEncoder
  implicit val decoder: Decoder[MultiSearchTermsAggregation] = deriveDecoder
}
