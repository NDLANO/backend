/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about search-results")
case class SearchResultV2(
    @description("The total number of learningpaths matching this query") totalCount: Long,
    @description("For which page results are shown from") page: Option[Int],
    @description("The number of results per page") pageSize: Int,
    @description("The chosen search language") language: String,
    @description("The search results") results: Seq[LearningPathSummaryV2]
)

object SearchResultV2 {
  implicit val encoder: Encoder[SearchResultV2] = deriveEncoder
  implicit val decoder: Decoder[SearchResultV2] = deriveDecoder
}
