/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.audioapi.model.api

import no.ndla.audioapi.model.Sort
import sttp.tapir.Schema.annotations.description

// format: off
@description("The search parameters")
case class SeriesSearchParamsDTO(
    @description("Return only series with titles, alt-texts or tags matching the specified query.") query: Option[String],
    @description("The ISO 639-1 language code describing language used in query-params") language: Option[String],
    @description("The page number of the search hits to display.") page: Option[Int],
    @description("The number of search hits to display for each page.") pageSize: Option[Int],
    @description("The sorting used on results. Default is by -relevance.") sort: Option[Sort],
    @description("A search context retrieved from the response header of a previous search.") scrollId: Option[String],
    @description("Return all matched series whether they exist on selected language or not.") fallback: Option[Boolean]
)
// format: on
