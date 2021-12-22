/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.learningpath

import no.ndla.searchapi.model.domain.LanguageField
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "The introduction of the learningpath")
case class Introduction(@(ApiModelProperty @field)(description =
                          "The introduction to the learningpath. Basic HTML allowed") introduction: String,
                        @(ApiModelProperty @field)(description =
                          "ISO 639-1 code that represents the language used in introduction") language: String)
    extends LanguageField
