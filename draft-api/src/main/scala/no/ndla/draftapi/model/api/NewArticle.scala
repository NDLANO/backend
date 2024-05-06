/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import no.ndla.common.implicits.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{DraftCopyright, RelatedContentLink}
import sttp.tapir.Schema.annotations.description

// format: off
@description("Information about the article")
case class NewArticle(
    @description("The chosen language") language: String,
    @description("The title of the article") title: String,
    @description("The date the article is published") published: Option[NDLADate],
    @description("The content of the article") content: Option[String],
    @description("Searchable tags") tags: Option[Seq[String]],
    @description("An introduction") introduction: Option[String],
    @description("A meta description") metaDescription: Option[String],
    @description("Meta image for the article") metaImage: Option[NewArticleMetaImage],
    @description("A visual element for the article. May be anything from an image to a video or H5P") visualElement: Option[String],
    @description("Describes the copyright information for the article") copyright: Option[DraftCopyright],
    @description("Required libraries in order to render the article") requiredLibraries: Option[Seq[RequiredLibrary]],
    @description("The type of article this is. Possible values are frontpage-article, standard, topic-article") articleType: String,
    @description("The notes for this article draft") notes: Option[Seq[String]],
    @description("The labels attached to this article; meant for editors.") editorLabels: Option[Seq[String]],
    @description("A list of codes from GREP API connected to the article") grepCodes: Option[Seq[String]],
    @description("A list of conceptIds connected to the article") conceptIds: Option[Seq[Long]],
    @description("Value that dictates who gets to see the article. Possible values are: everyone/teacher") availability: Option[String],
    @description("A list of content related to the article") relatedContent: Option[Seq[Either[RelatedContentLink, Long]]],
    @description("An object describing a future revision") revisionMeta: Option[Seq[RevisionMeta]],
    @description("NDLA ID representing the editor responsible for this article") responsibleId: Option[String],
    @description("The path to the frontpage article") slug: Option[String],
    @description("Information about a comment attached to an article") comments: Option[List[NewComment]],
    @description("If the article should be prioritized") prioritized: Option[Boolean],
    @description("If the article should be prioritized. Possible values are prioritized, on-hold, unspecified") priority: Option[String],
    @description("The quality evaluation of the article. Consist of a score from 1 to 5 and a comment.") qualityEvaluation : Option[QualityEvaluation],
)
// format: on

object NewArticle {
  implicit def eitherEnc: Encoder[Either[RelatedContentLink, Long]] = eitherEncoder[RelatedContentLink, Long]
  implicit def eitherDec: Decoder[Either[RelatedContentLink, Long]] = eitherDecoder[RelatedContentLink, Long]
  implicit def encoder: Encoder[NewArticle]                         = deriveEncoder
  implicit def decoder: Decoder[NewArticle]                         = deriveDecoder
}
