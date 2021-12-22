/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import java.util.Date
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Information about the concept")
case class ConceptSummary(
    @(ApiModelProperty @field)(description = "The unique id of the concept") id: Long,
    @(ApiModelProperty @field)(description = "Available titles for the concept") title: ConceptTitle,
    @(ApiModelProperty @field)(description = "The content of the concept in available languages") content: ConceptContent,
    @(ApiModelProperty @field)(description = "The metaImage of the concept") metaImage: ConceptMetaImage,
    @(ApiModelProperty @field)(description = "Search tags the concept is tagged with") tags: Option[ConceptTags],
    @(ApiModelProperty @field)(description = "Taxonomy subject ids the concept is connected to") subjectIds: Option[Set[String]],
    @(ApiModelProperty @field)(description = "All available languages of the current concept") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "The time when the article was last updated") lastUpdated: Date,
    @(ApiModelProperty @field)(description = "Status information of the concept") status: Status,
    @(ApiModelProperty @field)(description = "List of people that edited the concept") updatedBy: Seq[String],
    @(ApiModelProperty @field)(description = "Describes the license of the concept") license: Option[String])
// format: on
