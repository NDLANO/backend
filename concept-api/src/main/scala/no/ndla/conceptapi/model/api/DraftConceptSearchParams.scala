/*
 * Part of NDLA concept-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "The search parameters")
case class DraftConceptSearchParams(
  @(ApiModelProperty @field)(description = "The search query.") query: Option[String],
  @(ApiModelProperty @field)(description = "The ISO 639-1 language code describing language used in query-params.") language: Option[String],
  @(ApiModelProperty @field)(description = "The page number of the search hits to display.") page: Option[Int],
  @(ApiModelProperty @field)(description = "The number of search hits to display for each page.") pageSize: Option[Int],
  @(ApiModelProperty @field)(description = "Return only articles that have one of the provided ids.") idList: List[Long],
  @(ApiModelProperty @field)(description = "The sorting used on results. Default is by -relevance.") sort: Option[String],
  @(ApiModelProperty @field)(description = "Whether to fallback to existing language if not found in selected language.") fallback: Option[Boolean],
  @(ApiModelProperty @field)(description = "A search context retrieved from the response header of a previous search.") scrollId: Option[String],
  @(ApiModelProperty @field)(description = "A comma-separated list of subjects that should appear in the search.") subjects: Set[String],
  @(ApiModelProperty @field)(description = "A comma-separated list of tags to filter the search by.") tags: Set[String],
  @(ApiModelProperty @field)(description = "A comma-separated list of statuses that should appear in the search.") status: Set[String],
  @(ApiModelProperty @field)(description = "A comma-separated list of users to filter the search by.") users: Seq[String],
  @(ApiModelProperty @field)(description = "Embed resource type that should exist in the concepts.") embedResource: Option[String],
  @(ApiModelProperty @field)(description = "Embed id attribute that should exist in the concepts.") embedId: Option[String],
  @(ApiModelProperty @field)(description = "A comma-separated list of NDLA IDs to filter the search by.") responsibleIds: List[String],
  @(ApiModelProperty @field)(description = "The type of concepts to return.") conceptType: Option[String],
)
