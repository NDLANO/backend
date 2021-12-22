/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.article

import no.ndla.searchapi.model.domain.LanguageField
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Description of the article introduction")
case class ArticleIntroduction(
    @(ApiModelProperty @field)(description = "The introduction content") introduction: String,
    @(ApiModelProperty @field)(description =
      "The ISO 639-1 language code describing which article translation this introduction belongs to") language: String)
    extends LanguageField
