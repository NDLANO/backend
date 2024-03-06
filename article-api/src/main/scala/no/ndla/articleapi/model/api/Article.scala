/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.implicits._
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{Copyright, RelatedContent, RelatedContentLink}
import sttp.tapir.Schema.annotations.description

// format: off
@description("Information about the article")
case class ArticleV2(
    @description("The unique id of the article") id: Long,
    @description("Link to article on old platform") oldNdlaUrl: Option[String],
    @description("The revision number for the article") revision: Int,
    @description("Available titles for the article") title: ArticleTitle,
    @description("The content of the article in available languages") content: ArticleContentV2,
    @description("Describes the copyright information for the article") copyright: Copyright,
    @description("Searchable tags for the article") tags: ArticleTag,
    @description("Required libraries in order to render the article") requiredLibraries: Seq[RequiredLibrary],
    @description("A visual element article") visualElement: Option[VisualElement],
    @description("A meta image for the article") metaImage: Option[ArticleMetaImage],
    @description("An introduction for the article") introduction: Option[ArticleIntroduction],
    @description("Meta description for the article") metaDescription: ArticleMetaDescription,
    @description("When the article was created") created: NDLADate,
    @description("When the article was last updated") updated: NDLADate,
    @description("By whom the article was last updated") updatedBy: String,
    @description("When the article was last published") published: NDLADate,
    @description("The type of article this is. Possible values are frontpage-article, standard, topic-article") articleType: String,
    @description("The languages this article supports") supportedLanguages: Seq[String],
    @description("A list of codes from GREP API connected to the article") grepCodes: Seq[String],
    @description("A list of conceptIds connected to the article") conceptIds: Seq[Long],
    @description("Value that dictates who gets to see the article. Possible values are: everyone/teacher") availability: String,
    @description("A list of content related to the article") relatedContent: Seq[RelatedContent],
    @description("The date for the next planned revision which indicates when the article might be outdated") revisionDate: Option[NDLADate],
    @description("The path to the frontpage article") slug: Option[String],
)
// format: on

object ArticleV2 {
  implicit def eitherEnc: Encoder[Either[RelatedContentLink, Long]] = eitherEncoder[RelatedContentLink, Long]
  implicit def eitherDec: Decoder[Either[RelatedContentLink, Long]] = eitherDecoder[RelatedContentLink, Long]
  implicit val encoder: Encoder[ArticleV2]                          = deriveEncoder
  implicit val decoder: Decoder[ArticleV2]                          = deriveDecoder
}
