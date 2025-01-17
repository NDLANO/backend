/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.article

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.{NDLADate, RelatedContentLink}
import no.ndla.common.model.domain.*
import no.ndla.common.implicits.*
import no.ndla.common.model.domain.language.OptLanguageFields

case class Article(
    id: Option[Long],
    revision: Option[Int],
    title: Seq[Title],
    content: Seq[ArticleContent],
    copyright: Copyright,
    tags: Seq[Tag],
    requiredLibraries: Seq[RequiredLibrary],
    visualElement: Seq[VisualElement],
    introduction: Seq[Introduction],
    metaDescription: Seq[Description],
    metaImage: Seq[ArticleMetaImage],
    created: NDLADate,
    updated: NDLADate,
    updatedBy: String,
    published: NDLADate,
    articleType: ArticleType,
    grepCodes: Seq[String],
    conceptIds: Seq[Long],
    availability: Availability,
    relatedContent: Seq[RelatedContent],
    revisionDate: Option[NDLADate],
    slug: Option[String],
    disclaimer: OptLanguageFields[String]
) extends Content

object Article {
  implicit def eitherEnc: Encoder[Either[RelatedContentLink, Long]] = eitherEncoder[RelatedContentLink, Long]
  implicit def eitherDec: Decoder[Either[RelatedContentLink, Long]] = eitherDecoder[RelatedContentLink, Long]

  implicit def encoder: Encoder[Article] = deriveEncoder[Article]
  implicit def decoder: Decoder[Article] = deriveDecoder[Article]
}
