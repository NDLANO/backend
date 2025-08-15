/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.implicits.*
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{
  CommentDTO,
  DisclaimerDTO,
  DraftCopyrightDTO,
  RelatedContent,
  RelatedContentLinkDTO,
  ResponsibleDTO,
  RevisionMetaDTO
}
import sttp.tapir.Schema.annotations.description
import sttp.tapir.Schema
import no.ndla.common.model.domain.Priority
import no.ndla.common.DeriveHelpers

@description("Information about the article")
case class ArticleDTO(
    @description("The unique id of the article")
    id: Long,
    @description("Link to article on old platform")
    oldNdlaUrl: Option[String],
    @description("The revision number for the article")
    revision: Int,
    @description("The status of this article")
    status: StatusDTO,
    @description("Available titles for the article")
    title: Option[ArticleTitleDTO],
    @description("The content of the article in available languages")
    content: Option[ArticleContentDTO],
    @description("Describes the copyright information for the article")
    copyright: Option[DraftCopyrightDTO],
    @description("Searchable tags for the article")
    tags: Option[ArticleTagDTO],
    @description("Required libraries in order to render the article")
    requiredLibraries: Seq[RequiredLibraryDTO],
    @description("A visual element article")
    visualElement: Option[VisualElementDTO],
    @description("An introduction for the article")
    introduction: Option[ArticleIntroductionDTO],
    @description("Meta description for the article")
    metaDescription: Option[ArticleMetaDescriptionDTO],
    @description("Meta image for the article")
    metaImage: Option[ArticleMetaImageDTO],
    @description("When the article was created")
    created: NDLADate,
    @description("When the article was last updated")
    updated: NDLADate,
    @description("By whom the article was last updated")
    updatedBy: String,
    @description("When the article was last published")
    published: NDLADate,
    @description("The type of article this is. Possible values are frontpage-article, standard, topic-article")
    articleType: String,
    @description("The languages this article supports")
    supportedLanguages: Seq[String],
    @description("The notes for this article draft")
    notes: Seq[EditorNoteDTO],
    @description("The labels attached to this article; meant for editors.")
    editorLabels: Seq[String],
    @description("A list of codes from GREP API connected to the article")
    grepCodes: Seq[String],
    @description("A list of conceptIds connected to the article")
    conceptIds: Seq[Long],
    @description("Value that dictates who gets to see the article. Possible values are: everyone/teacher")
    availability: String,
    @description("A list of content related to the article")
    relatedContent: Seq[RelatedContent],
    @description("A list of revisions planned for the article")
    revisions: Seq[RevisionMetaDTO],
    @description("Object with data representing the editor responsible for this article")
    responsible: Option[ResponsibleDTO],
    @description("The path to the frontpage article")
    slug: Option[String],
    @description("Information about comments attached to the article")
    comments: Seq[CommentDTO],
    @description("If the article should be prioritized. Possible values are prioritized, on-hold, unspecified")
    priority: Priority,
    @description("If the article has been edited after last status or responsible change")
    started: Boolean,
    @description("The quality evaluation of the article. Consist of a score from 1 to 5 and a comment.")
    qualityEvaluation: Option[QualityEvaluationDTO],
    @description("The disclaimer of the article")
    disclaimer: Option[DisclaimerDTO]
)

object ArticleDTO {
  implicit def relatedContentEnc: Encoder[Either[RelatedContentLinkDTO, Long]] =
    eitherEncoder[RelatedContentLinkDTO, Long]
  implicit def relatedContentDec: Decoder[Either[RelatedContentLinkDTO, Long]] =
    eitherDecoder[RelatedContentLinkDTO, Long]

  implicit def encoder: Encoder[ArticleDTO] = deriveEncoder
  implicit def decoder: Decoder[ArticleDTO] = deriveDecoder
  implicit def schema: Schema[ArticleDTO]   = DeriveHelpers.getSchema
}
