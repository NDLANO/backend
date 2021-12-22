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

@ApiModel(description = "The description of the learningpath")
case class Description(
    @(ApiModelProperty @field)(description = "The description to the learningpath.") description: String,
    @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in introduction") language: String)
    extends LanguageField
