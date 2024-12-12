/*
 * Part of NDLA concept_api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about tags-search-results")
case class TagsSearchResultDTO(
    @description("The total number of tags matching this query") totalCount: Int,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[String]
)

object TagsSearchResultDTO {
  implicit val encoder: Encoder[TagsSearchResultDTO] = deriveEncoder
  implicit val decoder: Decoder[TagsSearchResultDTO] = deriveDecoder
}
