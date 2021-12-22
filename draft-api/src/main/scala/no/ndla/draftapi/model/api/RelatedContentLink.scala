/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "External link related to the article")
case class RelatedContentLink(
    @(ApiModelProperty @field)(description = "Title of the article") title: String,
    @(ApiModelProperty @field)(description = "The url to where the article can be viewed") url: String)
