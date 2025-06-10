/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.LanguageCode
import no.ndla.imageapi.model.domain.Sort
import sttp.tapir.Schema.annotations.{deprecated, description}

@description("The search parameters")
case class SearchParamsDTO(
    @description("Return only images with titles, alt-texts or tags matching the specified query.")
    query: Option[String],
    @description("Return only images with provided license.")
    license: Option[String],
    @description("The ISO 639-1 language code describing language used in query-params")
    language: Option[LanguageCode],
    @description("Fallback to existing language if language is specified.")
    fallback: Option[Boolean],
    @description("Return only images with full size larger than submitted value in bytes.")
    minimumSize: Option[Int],
    @deprecated @description("Return copyrighted images. May be omitted.")
    includeCopyrighted: Option[Boolean],
    @description(
      """The sorting used on results. The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id. Default is by -relevance (desc) when query is set, and title (asc) when query is empty."""
    )
    sort: Option[Sort],
    @description("The page number of the search hits to display.")
    page: Option[Int],
    @description("The number of search hits to display for each page.")
    pageSize: Option[Int],
    @description("Only show podcast friendly images.")
    podcastFriendly: Option[Boolean],
    @description("A search context retrieved from the response header of a previous search.")
    scrollId: Option[String],
    @description("Return only images with one of the provided values for modelReleased.")
    modelReleased: Option[Seq[String]],
    @description("Filter editors of the image(s). Multiple values can be specified in a comma separated list.")
    users: Option[List[String]]
)

object SearchParamsDTO {
  implicit val encoder: Encoder[SearchParamsDTO] = deriveEncoder
  implicit val decoder: Decoder[SearchParamsDTO] = deriveDecoder
}
