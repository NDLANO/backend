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

@ApiModel(description = "Information about the concept")
case class Concept(
    // format: off
    @(ApiModelProperty @field)(description = "The unique id of the concept") id: Long,
    @(ApiModelProperty @field)(description = "The revision of the concept") revision: Int,
    @(ApiModelProperty @field)(description = "Available titles for the concept") title: Option[ConceptTitle],
    @(ApiModelProperty @field)(description = "The content of the concept") content: Option[ConceptContent],
    @(ApiModelProperty @field)(description = "Describes the copyright information for the concept") copyright: Option[Copyright],
    @(ApiModelProperty @field)(description = "URL for the source of the concept") source: Option[String],
    @(ApiModelProperty @field)(description = "A meta image for the concept") metaImage: Option[ConceptMetaImage],
    @(ApiModelProperty @field)(description = "Search tags the concept is tagged with") tags: Option[ConceptTags],
    @(ApiModelProperty @field)(description = "Taxonomy subject ids the concept is connected to") subjectIds: Option[Set[String]],
    @(ApiModelProperty @field)(description = "When the concept was created") created: Date,
    @(ApiModelProperty @field)(description = "When the concept was last updated") updated: Date,
    @(ApiModelProperty @field)(description = "List of people that updated this concept") updatedBy: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "All available languages of the current concept") supportedLanguages: Set[String],
    @(ApiModelProperty @field)(description = "Article ids to which the concept is connected to") articleIds: Seq[Long],
    @(ApiModelProperty @field)(description = "Status information of the concept") status: Status,
    @(ApiModelProperty @field)(description = "A visual element for the concept") visualElement: Option[VisualElement]
    // format: on
)
