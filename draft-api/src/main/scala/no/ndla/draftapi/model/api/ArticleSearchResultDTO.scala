/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description
import no.ndla.common.DeriveHelpers

@description("Information about search-results")
case class ArticleSearchResultDTO(
    @description("The total number of articles matching this query")
    totalCount: Long,
    @description("For which page results are shown from")
    page: Option[Int],
    @description("The number of results per page")
    pageSize: Int,
    @description("The search results")
    results: Seq[ArticleSummaryDTO],
)

object ArticleSearchResultDTO {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}
  import sttp.tapir.generic.auto.*

  implicit val encoder: Encoder[ArticleSearchResultDTO]          = deriveEncoder
  implicit val decoder: Decoder[ArticleSearchResultDTO]          = deriveDecoder
  implicit def schema: sttp.tapir.Schema[ArticleSearchResultDTO] = DeriveHelpers.getSchema
}
