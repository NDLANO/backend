/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "Description of the tags of the article")
case class ArticleTag(@(ApiModelProperty @field)(description = "The searchable tag.") tags: Seq[String],
                      @(ApiModelProperty @field)(
                        description = "ISO 639-1 code that represents the language used in tag") language: String)
