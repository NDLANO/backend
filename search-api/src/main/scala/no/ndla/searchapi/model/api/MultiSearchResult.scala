/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Information about search-results")
case class MultiSearchResult(
    @(ApiModelProperty @field)(description = "The total number of resources matching this query") totalCount: Long,
    @(ApiModelProperty @field)(description = "For which page results are shown from") page: Option[Int],
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The chosen search language") language: String,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[MultiSearchSummary],
    @(ApiModelProperty @field)(description = "The suggestions for other searches") suggestions: Seq[MultiSearchSuggestion],
    @(ApiModelProperty @field)(description = "The aggregated fields if specified in query") aggregations: Seq[MultiSearchTermsAggregation]
)
// format: on
