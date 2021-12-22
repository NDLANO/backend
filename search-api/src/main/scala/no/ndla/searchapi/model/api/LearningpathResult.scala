/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Search result for learningpath api")
case class LearningpathResult(
    @(ApiModelProperty @field)(description = "The unique id of this learningpath") id: Long,
    @(ApiModelProperty @field)(description = "The title of the learningpath") title: Title,
    @(ApiModelProperty @field)(description = "The introduction of the learningpath") introduction: LearningPathIntroduction,
    @(ApiModelProperty @field)(description = "List of supported languages") supportedLanguages: Seq[String])
