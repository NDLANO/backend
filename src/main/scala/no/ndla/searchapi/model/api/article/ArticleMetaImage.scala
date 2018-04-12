/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.article

import no.ndla.searchapi.model.domain.LanguageField
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Meta description of the article")
case class ArticleMetaImage(@(ApiModelProperty@field)(description = "The meta image id") id: String,
                            @(ApiModelProperty@field)(description = "The ISO 639-1 language code describing which article translation this meta description belongs to") language: String)
  extends LanguageField[String] {
  override def value: String = id
}
