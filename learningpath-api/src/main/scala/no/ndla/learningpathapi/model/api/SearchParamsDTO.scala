/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.LanguageCode
import no.ndla.learningpathapi.model.domain.Sort
import sttp.tapir.Schema.annotations.description

@description("The search parameters")
case class SearchParamsDTO(
    @description("The search query")
    query: Option[String],
    @description("The ISO 639-1 language code describing language used in query-params")
    language: Option[LanguageCode],
    @description("The page number of the search hits to display.")
    page: Option[Int],
    @description("The number of search hits to display for each page.")
    pageSize: Option[Int],
    @description("Return only learning paths that have one of the provided ids")
    ids: Option[List[Long]],
    @description("Return only learning paths that are tagged with this exact tag.")
    tag: Option[String],
    @description("The sorting used on results. Default is by -relevance.")
    sort: Option[Sort],
    @description("Return all matched learning paths whether they exist on selected language or not.")
    fallback: Option[Boolean],
    @description("Return only learning paths that have the provided verification status.")
    verificationStatus: Option[String],
    @description("A search context retrieved from the response header of a previous search.")
    scrollId: Option[String]
)

object SearchParamsDTO {
  implicit val encoder: Encoder[SearchParamsDTO] = deriveEncoder
  implicit val decoder: Decoder[SearchParamsDTO] = deriveDecoder
}
