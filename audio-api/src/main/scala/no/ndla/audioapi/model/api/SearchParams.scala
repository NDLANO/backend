/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "The search parameters")
case class SearchParams(
    @(ApiModelProperty @field)(description = "Return only audio with titles, alt-texts or tags matching the specified query.") query: Option[String],
    @(ApiModelProperty @field)(description = "Return only audio with provided license.") license: Option[String],
    @(ApiModelProperty @field)(description = "The ISO 639-1 language code describing language used in query-params") language: Option[String],
    @(ApiModelProperty @field)(description = "The page number of the search hits to display.") page: Option[Int],
    @(ApiModelProperty @field)(description = "The number of search hits to display for each page.") pageSize: Option[Int],
    @(ApiModelProperty @field)(description = "The sorting used on results. Default is by -relevance.") sort: Option[String],
    @(ApiModelProperty @field)(description = "A search context retrieved from the response header of a previous search.") scrollId: Option[String],
    @(ApiModelProperty @field)(description = "Type of audio to filter by.") audioType: Option[String],
    @(ApiModelProperty @field)(description = "Filter result by whether they are a part of a series or not.\n'true' will return only audios that are a part of a series.\n'false' will return only audios that are NOT a part of a series.\nNot specifying will return both audios that are a part of a series and not.") filterBySeries: Option[Boolean],
    @(ApiModelProperty @field)(description = "Return all matched audios whether they exist on selected language or not.") fallback: Option[Boolean]
)
// format: on
