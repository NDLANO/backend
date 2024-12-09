/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.conceptapi.model.domain.Sort
import sttp.tapir.Schema.annotations.description

// format: off
@description("The search parameters")
case class DraftConceptSearchParamsDTO(
  @description("The search query.") query: Option[String],
  @description("The ISO 639-1 language code describing language used in query-params.") language: Option[String],
  @description("The page number of the search hits to display.") page: Option[Int],
  @description("The number of search hits to display for each page.") pageSize: Option[Int],
  @description("Return only articles that have one of the provided ids.") ids: Option[List[Long]],
  @description("The sorting used on results. Default is by -relevance.") sort: Option[Sort],
  @description("Whether to fallback to existing language if not found in selected language.") fallback: Option[Boolean],
  @description("A search context retrieved from the response header of a previous search.") scrollId: Option[String],
  @description("A comma-separated list of subjects that should appear in the search.") subjects: Option[Set[String]],
  @description("A comma-separated list of tags to filter the search by.") tags: Option[Set[String]],
  @description("A comma-separated list of statuses that should appear in the search.") status: Option[Set[String]],
  @description("A comma-separated list of users to filter the search by.") users: Option[Seq[String]],
  @description("Embed resource type that should exist in the concepts.") embedResource: Option[List[String]],
  @description("Embed id attribute that should exist in the concepts.") embedId: Option[String],
  @description("A comma-separated list of NDLA IDs to filter the search by.") responsibleIds: Option[List[String]],
  @description("The type of concepts to return.") conceptType: Option[String],
  @description("A list of index paths to aggregate over") aggregatePaths: Option[List[String]],
)

object DraftConceptSearchParamsDTO{
  implicit val encoder: Encoder[DraftConceptSearchParamsDTO] = deriveEncoder
  implicit val decoder: Decoder[DraftConceptSearchParamsDTO] = deriveDecoder
}
