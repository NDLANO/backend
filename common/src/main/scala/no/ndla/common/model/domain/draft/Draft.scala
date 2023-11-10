/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import enumeratum.Json4s
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain._
import no.ndla.language.Language.getSupportedLanguages
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers, JavaTypesSerializers}
import org.json4s.{DefaultFormats, Formats}

case class Draft(
    id: Option[Long],
    revision: Option[Int],
    status: Status,
    title: Seq[Title],
    content: Seq[ArticleContent],
    copyright: Option[DraftCopyright],
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
    comments: Seq[Comment],
    priority: Priority,
    started: Boolean
) extends Content {

  def supportedLanguages: Seq[String] =
    getSupportedLanguages(title, visualElement, introduction, metaDescription, tags, content, metaImage)
}

object Draft {
  val serializers = Seq(
    new EnumNameSerializer(Availability),
    Json4s.serializer(DraftStatus),
    Json4s.serializer(ArticleType),
    Json4s.serializer(RevisionStatus),
    Json4s.serializer(Priority),
    NDLADate.Json4sSerializer
  ) ++
    JavaTimeSerializers.all ++
    JavaTypesSerializers.all

  val jsonEncoder: Formats = DefaultFormats.withLong ++ serializers
}
