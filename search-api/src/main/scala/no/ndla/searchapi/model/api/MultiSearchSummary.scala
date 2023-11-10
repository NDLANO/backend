/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.draft.Comment
import no.ndla.common.model.domain.Priority
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Object describing matched field with matching words emphasized")
case class HighlightedField(
    @(ApiModelProperty @field)(description = "Field that matched") field: String,
    @(ApiModelProperty @field)(description = "List of segments that matched in `field`") matches: Seq[String]
)

// format: off
@ApiModel(description = "Short summary of information about the resource")
case class MultiSearchSummary(
    @(ApiModelProperty @field)(description = "The unique id of the resource") id: Long,
    @(ApiModelProperty @field)(description = "The title of the resource") title: Title,
    @(ApiModelProperty @field)(description = "The meta description of the resource") metaDescription: MetaDescription,
    @(ApiModelProperty @field)(description = "The meta image for the resource") metaImage: Option[MetaImage],
    @(ApiModelProperty @field)(description = "Url pointing to the resource") url: String,
    @(ApiModelProperty @field)(description = "Contexts of the resource") contexts: List[ApiTaxonomyContext],
    @(ApiModelProperty @field)(description = "Languages the resource exists in") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "Learning resource type, either 'standard', 'topic-article' or 'learningpath'") learningResourceType: String,
    @(ApiModelProperty @field)(description = "Status information of the resource") status: Option[Status],
    @(ApiModelProperty @field)(description = "Traits for the resource") traits: List[String],
    @(ApiModelProperty @field)(description = "Relevance score. The higher the score, the better the document matches your search criteria.") score: Float,
    @(ApiModelProperty @field)(description = "List of objects describing matched field with matching words emphasized") highlights: List[HighlightedField],
    @(ApiModelProperty @field)(description = "The taxonomy paths for the resource") paths: List[String],
    @(ApiModelProperty @field)(description = "The time and date of last update") lastUpdated: NDLADate,
    @(ApiModelProperty @field)(description = "Describes the license of the resource") license: Option[String],
    @(ApiModelProperty @field)(description = "A list of revisions planned for the article") revisions: Seq[RevisionMeta],
    @(ApiModelProperty @field)(description = "Responsible field") responsible: Option[DraftResponsible],
    @(ApiModelProperty @field)(description = "Information about comments attached to the article") comments: Option[Seq[Comment]],
    @(ApiModelProperty @field)(description ="If the article should be prioritized" ) priority: Priority
)
// format: on
