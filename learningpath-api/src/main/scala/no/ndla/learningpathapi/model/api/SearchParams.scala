/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "The search parameters")
case class SearchParams(
    @(ApiModelProperty @field)(description = "The search query") query: Option[String],
    @(ApiModelProperty @field)(
      description = "The ISO 639-1 language code describing language used in query-params"
    ) language: Option[String],
    @(ApiModelProperty @field)(description = "The page number of the search hits to display.") page: Option[Int],
    @(ApiModelProperty @field)(description = "The number of search hits to display for each page.") pageSize: Option[
      Int
    ],
    @(ApiModelProperty @field)(description = "Return only learning paths that have one of the provided ids") ids: List[
      Long
    ],
    @(ApiModelProperty @field)(
      description = "Return only learning paths that are tagged with this exact tag."
    ) tag: Option[String],
    @(ApiModelProperty @field)(description = "The sorting used on results. Default is by -relevance.") sort: Option[
      String
    ],
    @(ApiModelProperty @field)(
      description = "Return all matched learning paths whether they exist on selected language or not."
    ) fallback: Option[Boolean],
    @(ApiModelProperty @field)(
      description = "Return only learning paths that have the provided verification status."
    ) verificationStatus: Option[String],
    @(ApiModelProperty @field)(
      description = "A search context retrieved from the response header of a previous search."
    ) scrollId: Option[String]
)
