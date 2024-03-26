/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

// format: off
@description("The search parameters")
case class ArticleSearchParams(
    @description("The search query") query: Option[String],
    @description("The ISO 639-1 language code describing language used in query-params") language: Option[String],
    @description("Return only articles with provided license.") license: Option[String],
    @description("The page number of the search hits to display.") page: Option[Int],
    @description("The number of search hits to display for each page.") pageSize: Option[Int],
    @description("Return only articles that have one of the provided ids") ids: Option[List[Long]],
    @description("Return only articles of specific type(s)") articleTypes: Option[List[String]],
    @description("The sorting used on results. Default is by -relevance.") sort: Option[String],
    @description("A search context retrieved from the response header of a previous search.") scrollId: Option[String],
    @description("Fallback to some existing language if language is specified.") fallback: Option[Boolean],
    @description("Return only articles containing codes from GREP API") grepCodes: Option[List[String]]
)

object ArticleSearchParams {
  implicit def encoder: Encoder[ArticleSearchParams] = deriveEncoder
  implicit def decoder: Decoder[ArticleSearchParams] = deriveDecoder
}
