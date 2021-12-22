/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "The content of the article in the specified language")
case class ArticleContent(@(ApiModelProperty @field)(description = "The html content") content: String,
                          @(ApiModelProperty @field)(description =
                            "ISO 639-1 code that represents the language used in the content") language: String)
