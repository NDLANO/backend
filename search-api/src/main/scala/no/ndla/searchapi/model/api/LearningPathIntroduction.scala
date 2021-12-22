/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api
import no.ndla.searchapi.model.domain.LanguageField
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Introduction of learningPath")
case class LearningPathIntroduction(
    @(ApiModelProperty @field)(description = "The freetext introduction of the learningpath") introduction: String,
    @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in introduction") language: String)
    extends LanguageField
