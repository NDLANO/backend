/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about search-suggestions")
case class MultiSearchSuggestion(
    @(ApiModelProperty @field)(description = "The name of the field suggested for") name: String,
    @(ApiModelProperty @field)(description = "The list of suggestions for given field") suggestions: Seq[
      SearchSuggestion])

@ApiModel(description = "Search suggestion for query-text")
case class SearchSuggestion(
    @(ApiModelProperty @field)(description = "The search query suggestions are made for") text: String,
    @(ApiModelProperty @field)(description = "The offset in the search query") offset: Int,
    @(ApiModelProperty @field)(description = "The position index in the search query") length: Int,
    @(ApiModelProperty @field)(description = "The list of suggest options for the field") options: Seq[SuggestOption])

@ApiModel(description = "Search suggestion options for the terms in the query")
case class SuggestOption(@(ApiModelProperty @field)(description = "The suggested text") text: String,
                         @(ApiModelProperty @field)(description = "The score of the suggestion") score: Double)
