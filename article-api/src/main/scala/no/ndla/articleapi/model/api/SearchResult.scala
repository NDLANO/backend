/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Information about search-results")
case class SearchResultV2(
    @description("The total number of articles matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[ArticleSummaryV2]
)

@description("Information about tags-search-results")
case class TagsSearchResult(
    @description("The total number of tags matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[String]
)

@description("Information about articles")
case class ArticleDump(
    @description("The total number of articles in the database") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[ArticleV2]
)
