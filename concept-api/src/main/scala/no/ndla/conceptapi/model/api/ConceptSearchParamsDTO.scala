/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.LanguageCode
import no.ndla.conceptapi.model.domain.Sort
import sttp.tapir.Schema.annotations.description

@description("The search parameters")
case class ConceptSearchParamsDTO(
    @description("The search query.")
    query: Option[String],
    @description("The ISO 639-1 language code describing language used in query-params.")
    language: Option[LanguageCode],
    @description("The page number of the search hits to display.")
    page: Option[Int],
    @description("The number of search hits to display for each page.")
    pageSize: Option[Int],
    @description("Return only articles that have one of the provided ids.")
    ids: Option[List[Long]],
    @description("The sorting used on results. Default is by -relevance.")
    sort: Option[Sort],
    @description("Whether to fallback to existing language if not found in selected language.")
    fallback: Option[Boolean],
    @description("A search context retrieved from the response header of a previous search.")
    scrollId: Option[String],
    @description("A comma-separated list of tags to filter the search by.")
    tags: Option[Set[String]],
    @description("If provided, only return concept where query matches title exactly.")
    exactMatch: Option[Boolean],
    @description("Embed resource type that should exist in the concepts.")
    embedResource: Option[List[String]],
    @description("Embed id attribute that should exist in the concepts.")
    embedId: Option[String],
    @description("The type of concepts to return.")
    conceptType: Option[String],
    @description("A list of index paths to aggregate over")
    aggregatePaths: Option[List[String]]
)

object ConceptSearchParamsDTO {
  implicit val encoder: Encoder[ConceptSearchParamsDTO] = deriveEncoder
  implicit val decoder: Decoder[ConceptSearchParamsDTO] = deriveDecoder
}
