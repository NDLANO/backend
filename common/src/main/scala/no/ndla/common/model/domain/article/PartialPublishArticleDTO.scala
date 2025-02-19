/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.article

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import no.ndla.common.implicits.{eitherDecoder, eitherEncoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{RelatedContent, RelatedContentLinkDTO, UpdateOrDelete}
import no.ndla.common.model.domain.Availability
import sttp.tapir.Schema.annotations.description

// format: off
@description("Partial data about article to publish independently")
case class PartialPublishArticleDTO(
  @description("Value that dictates who gets to see the article. Possible values are: everyone/teacher") availability: Option[Availability],
  @description("A list of codes from GREP API connected to the article") grepCodes: Option[Seq[String]],
  @description("The name of the license") license: Option[String],
  @description("A list of meta description objects") metaDescription: Option[Seq[ArticleMetaDescriptionDTO]],
  @description("A list of content related to the article") relatedContent: Option[Seq[RelatedContent]],
  @description("A list of tag objects") tags: Option[Seq[ArticleTagDTO]],
  @description("A revision date to specify expected earliest revision date of the article") revisionDate:  UpdateOrDelete[NDLADate],
  @description("A date to specify published date of the article") published:  Option[NDLADate],
)

object PartialPublishArticleDTO {
  implicit val relatedContentLinkEnc: Encoder.AsObject[RelatedContentLinkDTO] = deriveEncoder[RelatedContentLinkDTO]
  implicit val relatedContentLinkDec: Decoder[RelatedContentLinkDTO] = deriveDecoder[RelatedContentLinkDTO]
  implicit def eitherEnc: Encoder[Either[RelatedContentLinkDTO, Long]] = eitherEncoder[RelatedContentLinkDTO, Long]
  implicit def eitherDec: Decoder[Either[RelatedContentLinkDTO, Long]] = eitherDecoder[RelatedContentLinkDTO, Long]
  implicit val encoder: Encoder.AsObject[PartialPublishArticleDTO] = UpdateOrDelete.filterMarkers(deriveEncoder[PartialPublishArticleDTO])
  implicit val decoder: Decoder[PartialPublishArticleDTO] = deriveDecoder[PartialPublishArticleDTO]
}
