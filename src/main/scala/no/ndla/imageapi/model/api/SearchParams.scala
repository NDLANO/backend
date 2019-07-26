/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "The search parameters")
case class SearchParams(
    @(ApiModelProperty @field)(description =
      "Return only images with titles, alt-texts or tags matching the specified query.") query: Option[String],
    @(ApiModelProperty @field)(description = "Return only images with provided license.") license: Option[String],
    @(ApiModelProperty @field)(description = "The ISO 639-1 language code describing language used in query-params") language: Option[
      String],
    @(ApiModelProperty @field)(description = "Return only images with full size larger than submitted value in bytes.") minimumSize: Option[
      Int],
    @(ApiModelProperty @field)(description = "Return copyrighted images. May be omitted.") includeCopyrighted: Option[
      Boolean],
    @(ApiModelProperty @field)(
      description =
        """The sorting used on results.
                                                                   The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
                                                                   Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""") sort: Option[
      String],
    @(ApiModelProperty @field)(description = "The page number of the search hits to display.") page: Option[Int],
    @(ApiModelProperty @field)(description = "The number of search hits to display for each page.") pageSize: Option[
      Int],
    @(ApiModelProperty @field)(
      description = "A search context retrieved from the response header of a previous search.") scrollId: Option[
      String]
)
