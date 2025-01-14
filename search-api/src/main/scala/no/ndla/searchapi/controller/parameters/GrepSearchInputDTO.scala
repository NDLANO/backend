/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller.parameters

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.network.tapir.NonEmptyString
import no.ndla.searchapi.model.api.grep.GrepSortDTO
import sttp.tapir.Schema.annotations.description

@description("Input parameters to subject aggregations endpoint")
case class GrepSearchInputDTO(
    @description("A comma separated list of prefixes that should be returned in the search.")
    prefixFilter: Option[List[String]],
    @description("A comma separated list of codes that should be returned in the search.")
    codes: Option[List[String]],
    @description("A query to filter the query by.")
    query: Option[NonEmptyString],
    @description("The page number of the search hits to display.")
    page: Option[Int],
    @description(s"The number of search hits to display for each page.")
    pageSize: Option[Int],
    @description("The sort order of the search hits.")
    sort: Option[GrepSortDTO],
    @description("The ISO 639-1 language code describing language used in query-params")
    language: Option[String]
)

object GrepSearchInputDTO {
  implicit val encoder: Encoder[GrepSearchInputDTO] = deriveEncoder
  implicit val decoder: Decoder[GrepSearchInputDTO] = deriveDecoder
}
