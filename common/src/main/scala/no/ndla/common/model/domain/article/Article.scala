/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.article

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain._

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
    availability: Availability.Value = Availability.everyone,
    relatedContent: Seq[RelatedContent],
    revisionDate: Option[NDLADate],
    slug: Option[String]
) extends Content
