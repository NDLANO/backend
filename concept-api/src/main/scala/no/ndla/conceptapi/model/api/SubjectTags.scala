/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "A subject id, and list of tags used in the subject")
case class SubjectTags(
    @(ApiModelProperty @field)(description = "Taxonomy id of the subject") subjectId: String,
    @(ApiModelProperty @field)(description = "List of tags used in the subject") tags: List[String],
    @(ApiModelProperty @field)(description = "Language for the specified tags") language: String
)
