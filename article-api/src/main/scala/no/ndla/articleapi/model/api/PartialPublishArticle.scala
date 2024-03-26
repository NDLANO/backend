/*
 * Part of NDLA article-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import no.ndla.common.implicits.{eitherDecoder, eitherEncoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{RelatedContent, RelatedContentLink, UpdateOrDelete}
import no.ndla.common.model.domain.Availability
import sttp.tapir.Schema.annotations.description

// format: off
@description("Partial data about article to publish independently")
case class PartialPublishArticle(
    @description("Value that dictates who gets to see the article. Possible values are: everyone/teacher") availability: Option[Availability],
    @description("A list of codes from GREP API connected to the article") grepCodes: Option[Seq[String]],
    @description("The name of the license") license: Option[String],
    @description("A list of meta description objects") metaDescription: Option[Seq[ArticleMetaDescription]],
    @description("A list of content related to the article") relatedContent: Option[Seq[RelatedContent]],
    @description("A list of tag objects") tags: Option[Seq[ArticleTag]],
    @description("A revision date to specify expected earliest revision date of the article") revisionDate:  UpdateOrDelete[NDLADate],
)

object PartialPublishArticle {
  implicit val relatedContentLinkEnc: Encoder.AsObject[RelatedContentLink] = deriveEncoder[RelatedContentLink]
  implicit val relatedContentLinkDec: Decoder[RelatedContentLink] = deriveDecoder[RelatedContentLink]
  implicit def eitherEnc: Encoder[Either[RelatedContentLink, Long]] = eitherEncoder[RelatedContentLink, Long]
  implicit def eitherDec: Decoder[Either[RelatedContentLink, Long]] = eitherDecoder[RelatedContentLink, Long]
  implicit val encoder: Encoder.AsObject[PartialPublishArticle] = UpdateOrDelete.filterMarkers(deriveEncoder[PartialPublishArticle])
  implicit val decoder: Decoder[PartialPublishArticle] = deriveDecoder[PartialPublishArticle]
}
