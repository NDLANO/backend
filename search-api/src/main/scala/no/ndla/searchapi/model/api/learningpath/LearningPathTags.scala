/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.learningpath

import no.ndla.searchapi.model.domain.LanguageField
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

case class LearningPathTags(
    @(ApiModelProperty @field)(description = "The searchable tags. Must be plain text") tags: Seq[String],
    @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in tag") language: String)
    extends LanguageField
