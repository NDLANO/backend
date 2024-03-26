/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.implicits._
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.draft.Comment
import no.ndla.common.model.api.{DraftCopyright, RelatedContent, RelatedContentLink}
import sttp.tapir.Schema.annotations.description

// format: off
@description("Information about the article")
case class Article(
    @description("The unique id of the article") id: Long,
    @description("Link to article on old platform") oldNdlaUrl: Option[String],
    @description("The revision number for the article") revision: Int,
    @description("The status of this article") status: Status,
    @description("Available titles for the article") title: Option[ArticleTitle],
    @description("The content of the article in available languages") content: Option[ArticleContent],
    @description("Describes the copyright information for the article") copyright: Option[DraftCopyright],
    @description("Searchable tags for the article") tags: Option[ArticleTag],
    @description("Required libraries in order to render the article") requiredLibraries: Seq[RequiredLibrary],
    @description("A visual element article") visualElement: Option[VisualElement],
    @description("An introduction for the article") introduction: Option[ArticleIntroduction],
    @description("Meta description for the article") metaDescription: Option[ArticleMetaDescription],
    @description("Meta image for the article") metaImage: Option[ArticleMetaImage],
    @description("When the article was created") created: NDLADate,
    @description("When the article was last updated") updated: NDLADate,
    @description("By whom the article was last updated") updatedBy: String,
    @description("When the article was last published") published: NDLADate,
    @description("The type of article this is. Possible values are frontpage-article, standard, topic-article") articleType: String,
    @description("The languages this article supports") supportedLanguages: Seq[String],
    @description("The notes for this article draft") notes: Seq[EditorNote],
    @description("The labels attached to this article; meant for editors.") editorLabels: Seq[String],
    @description("A list of codes from GREP API connected to the article") grepCodes: Seq[String],
    @description("A list of conceptIds connected to the article") conceptIds: Seq[Long],
    @description("Value that dictates who gets to see the article. Possible values are: everyone/teacher") availability: String,
    @description("A list of content related to the article") relatedContent: Seq[RelatedContent],
    @description("A list of revisions planned for the article") revisions: Seq[RevisionMeta],
    @description("Object with data representing the editor responsible for this article") responsible: Option[DraftResponsible],
    @description("The path to the frontpage article") slug: Option[String],
    @description("Information about comments attached to the article") comments: Seq[Comment],
    @description("If the article should be prioritized") prioritized: Boolean,
    @description("If the article should be prioritized. Possible values are prioritized, on-hold, unspecified") priority: String,
    @description("If the article has been edited after last status or responsible change") started: Boolean
)

object Article {
  implicit def relatedContentEnc: Encoder[Either[RelatedContentLink, Long]] = eitherEncoder[RelatedContentLink, Long]
  implicit def relatedContentDec: Decoder[Either[RelatedContentLink, Long]] = eitherDecoder[RelatedContentLink, Long]

  implicit def encoder: Encoder[Article] = deriveEncoder
  implicit def decoder: Decoder[Article] = deriveDecoder
}
