/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

import no.ndla.searchapi.model.domain.article
import no.ndla.searchapi.model.domain.draft
import no.ndla.searchapi.model.domain.{Content, Tag, Title}
import org.joda.time.DateTime

case class Draft(
    id: Option[Long],
    revision: Option[Int],
    status: Status,
    title: Seq[Title],
    content: Seq[article.ArticleContent],
    copyright: Option[draft.Copyright],
    tags: Seq[Tag],
    requiredLibraries: Seq[article.RequiredLibrary],
    visualElement: Seq[article.VisualElement],
    introduction: Seq[article.ArticleIntroduction],
    metaDescription: Seq[article.MetaDescription],
    metaImage: Seq[article.ArticleMetaImage],
    created: DateTime,
    updated: DateTime,
    updatedBy: String,
    published: DateTime,
    articleType: article.LearningResourceType.Value,
    notes: List[EditorNote],
    previousVersionsNotes: List[EditorNote],
    grepCodes: Seq[String]
) extends Content
