/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import java.time.LocalDateTime
import no.ndla.language.Language.getSupportedLanguages
import no.ndla.common.model.domain.{
  ArticleContent,
  ArticleMetaImage,
  ArticleType,
  Availability,
  Content,
  Description,
  EditorNote,
  Introduction,
  RelatedContent,
  RequiredLibrary,
  Status,
  Tag,
  Title,
  VisualElement
}

case class Draft(
    id: Option[Long],
    revision: Option[Int],
    status: Status,
    title: Seq[Title],
    content: Seq[ArticleContent],
    copyright: Option[Copyright],
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
    articleType: ArticleType,
    notes: Seq[EditorNote],
    previousVersionsNotes: Seq[EditorNote],
    editorLabels: Seq[String],
    grepCodes: Seq[String],
    conceptIds: Seq[Long],
    availability: Availability.Value = Availability.everyone,
    relatedContent: Seq[RelatedContent],
    revisionMeta: Seq[RevisionMeta],
    responsible: Option[DraftResponsible]
) extends Content {

  def supportedLanguages: Seq[String] =
    getSupportedLanguages(title, visualElement, introduction, metaDescription, tags, content, metaImage)
}
