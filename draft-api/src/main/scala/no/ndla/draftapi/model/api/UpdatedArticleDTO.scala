/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import no.ndla.common.implicits._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{DraftCopyrightDTO, RelatedContent, RelatedContentLinkDTO, UpdateOrDelete}
import sttp.tapir.Schema.annotations.description

// format: off
@description("Information about the article")
case class UpdatedArticleDTO(
    @description("The revision number for the article") revision: Int,
    @description("The chosen language") language: Option[String],
    @description("The title of the article") title: Option[String],
    @description("The status of the article") status: Option[String],
    @description("The date the article is published") published: Option[NDLADate],
    @description("The content of the article") content: Option[String],
    @description("Searchable tags") tags: Option[Seq[String]],
    @description("An introduction") introduction: Option[String],
    @description("A meta description") metaDescription: Option[String],
    @description("An image-api ID for the article meta image") metaImage: UpdateOrDelete[NewArticleMetaImageDTO],
    @description("A visual element for the article. May be anything from an image to a video or H5P") visualElement: Option[String],
    @description("Describes the copyright information for the article") copyright: Option[DraftCopyrightDTO],
    @description("Required libraries in order to render the article") requiredLibraries: Option[Seq[RequiredLibraryDTO]],
    @description("The type of article this is. Possible values are frontpage-article, standard, topic-article") articleType: Option[String],
    @description("The notes for this article draft") notes: Option[Seq[String]],
    @description("The labels attached to this article; meant for editors.") editorLabels: Option[Seq[String]],
    @description("A list of codes from GREP API connected to the article") grepCodes: Option[Seq[String]],
    @description("A list of conceptIds connected to the article") conceptIds: Option[Seq[Long]],
    @description("Stores the new article as a separate version. Useful when making big changes that should be revertable.") createNewVersion: Option[Boolean],
    @description("Value that dictates who gets to see the article. Possible values are: everyone/teacher") availability: Option[String],
    @description("A list of content related to the article") relatedContent: Option[Seq[Either[RelatedContentLinkDTO, Long]]],
    @description("A list of all revisions of the article") revisionMeta: Option[Seq[RevisionMetaDTO]],
    @description("NDLA ID representing the editor responsible for this article") responsibleId: UpdateOrDelete[String],
    @description("The path to the frontpage article") slug: Option[String],
    @description("Information about a comment attached to an article") comments: Option[List[UpdatedCommentDTO]],
    @description("If the article should be prioritized") prioritized: Option[Boolean],
    @description("If the article should be prioritized. Possible values are prioritized, on-hold, unspecified") priority: Option[String],
    @description("The quality evaluation of the article. Consist of a score from 1 to 5 and a comment.") qualityEvaluation : Option[QualityEvaluationDTO],
    @description("The disclaimer of the article") disclaimer: Option[String]
)
// format: on

object UpdatedArticleDTO {
  implicit def relatedContentEncoder: Encoder[RelatedContent] = eitherEncoder[RelatedContentLinkDTO, Long]
  implicit def relatedContentDecoder: Decoder[RelatedContent] = eitherDecoder[RelatedContentLinkDTO, Long]

  implicit def encoder: Encoder[UpdatedArticleDTO] = deriveEncoder[UpdatedArticleDTO]
  implicit def decoder: Decoder[UpdatedArticleDTO] = deriveDecoder[UpdatedArticleDTO]
}
