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
@ApiModel(description = "Information about the gloss example")
case class GlossExample(
    @(ApiModelProperty @field)(description = "Example use of the gloss") example: String,
    @(ApiModelProperty @field)(description = "Language of the example") language: String,
)

@ApiModel(description = "Information about the gloss data")
case class GlossData(
    @(ApiModelProperty @field)(description = "Type of gloss, ex. noun, adjective, verb, adverb, ...") glossType: String,
    @(ApiModelProperty @field)(description = "Original language of the gloss") originalLanguage: String,
    @(ApiModelProperty @field)(description = "Alternative writing of the gloss") alternatives: Map[String, String],
    @(ApiModelProperty @field)(description = "List of examples of how the gloss can be used") examples: List[List[GlossExample]]
)
// format: on
