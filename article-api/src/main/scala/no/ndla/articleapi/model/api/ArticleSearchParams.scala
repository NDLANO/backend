/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.api

import no.ndla.articleapi.model.domain.Sort
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
    @description("The sorting used on results. Default is by -relevance.") sort: Option[Sort],
    @description("Return all matched articles whether they exist on selected language or not.") fallback: Option[Boolean],
    @description("A search context retrieved from the response header of a previous search.") scrollId: Option[String],
    @description("A comma separated list of codes from GREP API to filter by.") grepCodes: Option[Seq[String]]
)
// format: on
