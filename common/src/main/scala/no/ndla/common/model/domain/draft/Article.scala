/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import java.time.LocalDateTime
import enumeratum.{Enum, EnumEntry}
import no.ndla.common.errors.ValidationException
import no.ndla.language.Language.getSupportedLanguages
import no.ndla.common.model.domain.{
  ArticleContent,
  ArticleIntroduction,
  ArticleMetaDescription,
  ArticleMetaImage,
  ArticleTag,
  ArticleTitle,
  Availability,
  Content,
  EditorNote,
  RelatedContent,
  RequiredLibrary,
  Status,
  VisualElement
}

case class Article(
    id: Option[Long],
    revision: Option[Int],
    status: Status,
    title: Seq[ArticleTitle],
    content: Seq[ArticleContent],
    copyright: Option[Copyright],
    tags: Seq[ArticleTag],
    requiredLibraries: Seq[RequiredLibrary],
    visualElement: Seq[VisualElement],
    introduction: Seq[ArticleIntroduction],
    metaDescription: Seq[ArticleMetaDescription],
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
    revisionMeta: Seq[RevisionMeta]
) extends Content {

  def supportedLanguages: Seq[String] =
    getSupportedLanguages(title, visualElement, introduction, metaDescription, tags, content, metaImage)
}

sealed abstract class ArticleType(override val entryName: String) extends EnumEntry {
  override def toString: String = super.toString
}

object ArticleType extends Enum[ArticleType] {
  case object Standard     extends ArticleType("standard")
  case object TopicArticle extends ArticleType("topic-article")

  val values: IndexedSeq[ArticleType] = findValues

  def all: Seq[String]                        = ArticleType.values.map(_.entryName)
  def valueOf(s: String): Option[ArticleType] = ArticleType.withNameOption(s)

  def valueOfOrError(s: String): ArticleType =
    valueOf(s).getOrElse(
      throw ValidationException(
        "articleType",
        s"'$s' is not a valid article type. Valid options are ${all.mkString(",")}."
      )
    )
}
