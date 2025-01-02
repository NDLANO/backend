/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Information about audio summary search-results")
case class AudioSummarySearchResultDTO(
    @description("The total number of articles matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[AudioSummaryDTO]
)

@description("Information about series summary search-results")
case class SeriesSummarySearchResultDTO(
    @description("The total number of articles matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[SeriesSummaryDTO]
)

@description("Information about tags-search-results")
case class TagsSearchResultDTO(
    @description("The total number of tags matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[String]
)
