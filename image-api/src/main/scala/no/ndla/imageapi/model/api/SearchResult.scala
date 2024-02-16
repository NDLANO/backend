/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about search-results")
case class SearchResult(
    @description("The total number of images matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[ImageMetaSummary]
)

object SearchResult {
  implicit val encoder: Encoder[SearchResult] = deriveEncoder
  implicit val decoder: Decoder[SearchResult] = deriveDecoder
}

@description("Information about search-results")
case class SearchResultV3(
    @description("The total number of images matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[ImageMetaInformationV3]
)

object SearchResultV3 {
  implicit val encoder: Encoder[SearchResultV3] = deriveEncoder
  implicit val decoder: Decoder[SearchResultV3] = deriveDecoder
}

@description("Information about tags-search-results")
case class TagsSearchResult(
    @description("The total number of tags matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[String]
)

object TagsSearchResult {
  implicit val encoder: Encoder[TagsSearchResult] = deriveEncoder
  implicit val decoder: Decoder[TagsSearchResult] = deriveDecoder
}
