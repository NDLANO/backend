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
case class SearchResultDTO(
    @description("The total number of images matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[ImageMetaSummaryDTO]
)

object SearchResultDTO {
  implicit val encoder: Encoder[SearchResultDTO] = deriveEncoder
  implicit val decoder: Decoder[SearchResultDTO] = deriveDecoder
}

@description("Information about search-results")
case class SearchResultV3DTO(
    @description("The total number of images matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[ImageMetaInformationV3DTO]
)

object SearchResultV3DTO {
  implicit val encoder: Encoder[SearchResultV3DTO] = deriveEncoder
  implicit val decoder: Decoder[SearchResultV3DTO] = deriveDecoder
}

@description("Information about tags-search-results")
case class TagsSearchResultDTO(
    @description("The total number of tags matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[String]
)

object TagsSearchResultDTO {
  implicit val encoder: Encoder[TagsSearchResultDTO] = deriveEncoder
  implicit val decoder: Decoder[TagsSearchResultDTO] = deriveDecoder
}
