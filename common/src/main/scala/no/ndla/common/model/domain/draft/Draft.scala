/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import enumeratum.Json4s

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
  Responsible,
  Status,
  Tag,
  Title,
  VisualElement
}
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers, JavaTypesSerializers}
import org.json4s.{DefaultFormats, Formats}

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
    responsible: Option[Responsible],
    slug: Option[String],
    comments: Seq[Comment]
) extends Content {

  def supportedLanguages: Seq[String] =
    getSupportedLanguages(title, visualElement, introduction, metaDescription, tags, content, metaImage)
}

object Draft {
  val serializers = Seq(
    new EnumNameSerializer(DraftStatus),
    Json4s.serializer(ArticleType),
    Json4s.serializer(RevisionStatus),
    new EnumNameSerializer(Availability)
  ) ++
    JavaTimeSerializers.all ++
    JavaTypesSerializers.all

  val jsonEncoder: Formats = DefaultFormats.withLong ++ serializers
}
