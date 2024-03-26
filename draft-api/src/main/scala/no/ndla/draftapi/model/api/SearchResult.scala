/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about search-results")
case class SearchResult(
    @description("The total number of articles matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[ArticleSummary]
)

@description("Information and metadata about codes from GREP API")
case class GrepCodesSearchResult(
    @description("The total number of codes from GREP API matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The search results") results: Seq[String]
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
    @description("The search results") results: Seq[Article]
)

object ArticleDump {
  implicit def encoder: Encoder[ArticleDump] = deriveEncoder
  implicit def decoder: Decoder[ArticleDump] = deriveDecoder
}
