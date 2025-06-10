/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import no.ndla.audioapi.model.Sort
import no.ndla.common.model.api.LanguageCode
import sttp.tapir.Schema.annotations.description

@description("The search parameters")
case class SearchParamsDTO(
    @description("Return only audio with titles, alt-texts or tags matching the specified query.")
    query: Option[String],
    @description("Return only audio with provided license.")
    license: Option[String],
    @description("The ISO 639-1 language code describing language used in query-params")
    language: Option[LanguageCode],
    @description("The page number of the search hits to display.")
    page: Option[Int],
    @description("The number of search hits to display for each page.")
    pageSize: Option[Int],
    @description("The sorting used on results. Default is by -relevance.")
    sort: Option[Sort],
    @description("A search context retrieved from the response header of a previous search.")
    scrollId: Option[String],
    @description("Type of audio to filter by.")
    audioType: Option[String],
    @description(
      "Filter result by whether they are a part of a series or not.\n'true' will return only audios that are a part of a series.\n'false' will return only audios that are NOT a part of a series.\nNot specifying will return both audios that are a part of a series and not."
    )
    filterBySeries: Option[Boolean],
    @description("Return all matched audios whether they exist on selected language or not.")
    fallback: Option[Boolean]
)
