/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.DraftCopyright

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
    @(ApiModelProperty @field)(description = "The time when the article was last updated") lastUpdated: NDLADate,
    @(ApiModelProperty @field)(description = "When the concept was created") created: NDLADate,
    @(ApiModelProperty @field)(description = "Status information of the concept") status: Status,
    @(ApiModelProperty @field)(description = "List of people that edited the concept") updatedBy: Seq[String],
    @(ApiModelProperty @field)(description = "Describes the license of the concept") license: Option[String],
    @(ApiModelProperty @field)(description = "Describes the copyright of the concept") copyright: Option[DraftCopyright],
    @(ApiModelProperty @field)(description = "A visual element for the concept") visualElement: Option[VisualElement],
    @(ApiModelProperty @field)(description = "Article ids to which the concept is connected to") articleIds: Seq[Long],
    @(ApiModelProperty @field)(description = "URL for the source of the concept") source: Option[String],
    @(ApiModelProperty @field)(description = "Object with data representing the editor responsible for this concept") responsible: Option[ConceptResponsible],
    @(ApiModelProperty @field)(description = "Type of concept. 'concept', or 'gloss'") conceptType: String,
    @(ApiModelProperty @field)(description = "Information about the gloss") glossData: Option[GlossData]
)
// format: on
