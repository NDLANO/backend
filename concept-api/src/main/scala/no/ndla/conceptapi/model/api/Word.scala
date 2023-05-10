/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

// format: off
@ApiModel(description = "Information about the word example")
case class WordExample(
    @(ApiModelProperty @field)(description = "Example use of the word") example: String,
    @(ApiModelProperty @field)(description = "Language of the example") language: String,
)

@ApiModel(description = "Information about the word list")
case class WordList(
    @(ApiModelProperty @field)(description = "Word class/part of speech, ex. noun, adjective, verb, adverb, ...") wordType: String,
    @(ApiModelProperty @field)(description = "Words original language") originalLanguage: String,
    @(ApiModelProperty @field)(description = "List of examples of how the word can be used") examples: List[List[WordExample]]
)
// format: on
