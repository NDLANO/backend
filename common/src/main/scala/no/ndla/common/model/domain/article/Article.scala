/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.article

import no.ndla.common.model.domain.{
  ArticleContent,
  ArticleMetaImage,
  Availability,
  Content,
  Description,
  Introduction,
  RelatedContent,
  RequiredLibrary,
  Tag,
  Title,
  VisualElement
}

import java.time.LocalDateTime

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
    created: LocalDateTime,
    updated: LocalDateTime,
    updatedBy: String,
    published: LocalDateTime,
    articleType: String,
    grepCodes: Seq[String],
    conceptIds: Seq[Long],
    availability: Availability.Value = Availability.everyone,
    relatedContent: Seq[RelatedContent],
    revisionDate: Option[LocalDateTime],
    slug: Option[String]
) extends Content
