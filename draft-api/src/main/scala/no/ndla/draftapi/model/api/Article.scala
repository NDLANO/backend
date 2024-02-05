/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{DraftCopyright, RelatedContent, RelatedContentLink}
import no.ndla.common.model.api.draft.Comment
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field
import no.ndla.common.implicits._

// format: off
@ApiModel(description = "Information about the article")
case class Article(
    @(ApiModelProperty @field)(description = "The unique id of the article") id: Long,
    @(ApiModelProperty @field)(description = "Link to article on old platform") oldNdlaUrl: Option[String],
    @(ApiModelProperty @field)(description = "The revision number for the article") revision: Int,
    @(ApiModelProperty @field)(description = "The status of this article", allowableValues = "CREATED,IMPORTED,DRAFT,SKETCH,USER_TEST,QUALITY_ASSURED,AWAITING_QUALITY_ASSURANCE") status: Status,
    @(ApiModelProperty @field)(description = "Available titles for the article") title: Option[ArticleTitle],
    @(ApiModelProperty @field)(description = "The content of the article in available languages") content: Option[ArticleContent],
    @(ApiModelProperty @field)(description = "Describes the copyright information for the article") copyright: Option[DraftCopyright],
    @(ApiModelProperty @field)(description = "Searchable tags for the article") tags: Option[ArticleTag],
    @(ApiModelProperty @field)(description = "Required libraries in order to render the article") requiredLibraries: Seq[RequiredLibrary],
    @(ApiModelProperty @field)(description = "A visual element article") visualElement: Option[VisualElement],
    @(ApiModelProperty @field)(description = "An introduction for the article") introduction: Option[ArticleIntroduction],
    @(ApiModelProperty @field)(description = "Meta description for the article") metaDescription: Option[ArticleMetaDescription],
    @(ApiModelProperty @field)(description = "Meta image for the article") metaImage: Option[ArticleMetaImage],
    @(ApiModelProperty @field)(description = "When the article was created") created: NDLADate,
    @(ApiModelProperty @field)(description = "When the article was last updated") updated: NDLADate,
    @(ApiModelProperty @field)(description = "By whom the article was last updated") updatedBy: String,
    @(ApiModelProperty @field)(description = "When the article was last published") published: NDLADate,
    @(ApiModelProperty @field)(description = "The type of article this is. Possible values are frontpage-article, standard, topic-article") articleType: String,
    @(ApiModelProperty @field)(description = "The languages this article supports") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "The notes for this article draft") notes: Seq[EditorNote],
    @(ApiModelProperty @field)(description = "The labels attached to this article; meant for editors.") editorLabels: Seq[String],
    @(ApiModelProperty @field)(description = "A list of codes from GREP API connected to the article") grepCodes: Seq[String],
    @(ApiModelProperty @field)(description = "A list of conceptIds connected to the article") conceptIds: Seq[Long],
    @(ApiModelProperty @field)(description = "Value that dictates who gets to see the article. Possible values are: everyone/teacher") availability: String,
    @(ApiModelProperty @field)(description = "A list of content related to the article") relatedContent: Seq[RelatedContent],
    @(ApiModelProperty @field)(description = "A list of revisions planned for the article") revisions: Seq[RevisionMeta],
    @(ApiModelProperty @field)(description = "Object with data representing the editor responsible for this article") responsible: Option[DraftResponsible],
    @(ApiModelProperty @field)(description = "The path to the frontpage article") slug: Option[String],
    @(ApiModelProperty @field)(description = "Information about comments attached to the article") comments: Seq[Comment],
    @(ApiModelProperty @field)(description = "If the article should be prioritized") prioritized: Boolean,
    @(ApiModelProperty @field)(description = "If the article should be prioritized. Possible values are prioritized, on-hold, unspecified") priority: String,
    @(ApiModelProperty @field)(description = "If the article has been edited after last status or responsible change") started: Boolean
)

object Article {
  implicit def relatedContentEnc: Encoder[Either[RelatedContentLink, Long]] = eitherEncoder[RelatedContentLink, Long]
  implicit def relatedContentDec: Decoder[Either[RelatedContentLink, Long]] = eitherDecoder[RelatedContentLink, Long]

  implicit def encoder: Encoder[Article] = deriveEncoder
  implicit def decoder: Decoder[Article] = deriveDecoder
}
