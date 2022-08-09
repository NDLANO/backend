/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "The search parameters")
case class SeriesSearchParams(
    @(ApiModelProperty @field)(description = "Return only series with titles, alt-texts or tags matching the specified query.") query: Option[String],
    @(ApiModelProperty @field)(description = "The ISO 639-1 language code describing language used in query-params") language: Option[String],
    @(ApiModelProperty @field)(description = "The page number of the search hits to display.") page: Option[Int],
    @(ApiModelProperty @field)(description = "The number of search hits to display for each page.") pageSize: Option[Int],
    @(ApiModelProperty @field)(description = "The sorting used on results. Default is by -relevance.") sort: Option[String],
    @(ApiModelProperty @field)(description = "A search context retrieved from the response header of a previous search.") scrollId: Option[String],
    @(ApiModelProperty @field)(description = "Return all matched series whether they exist on selected language or not.") fallback: Option[Boolean]
)
// format: on
