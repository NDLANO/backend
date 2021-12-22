/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "Description of a title")
case class ArticleTitle(@(ApiModelProperty @field)(description = "The freetext title of the article") title: String,
                        @(ApiModelProperty @field)(
                          description = "ISO 639-1 code that represents the language used in title") language: String)
