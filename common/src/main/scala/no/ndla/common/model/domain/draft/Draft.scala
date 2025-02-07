/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.draft

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.implicits.*
import no.ndla.common.model.{NDLADate, RelatedContentLink}
import no.ndla.common.model.domain.*
import no.ndla.common.model.domain.language.OptLanguageFields
import no.ndla.language.Language.getSupportedLanguages

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
    availability: Availability = Availability.everyone,
    relatedContent: Seq[RelatedContent],
    revisionMeta: Seq[RevisionMeta],
    responsible: Option[Responsible],
    slug: Option[String],
    comments: Seq[Comment],
    priority: Priority,
    started: Boolean,
    qualityEvaluation: Option[QualityEvaluation],
    disclaimer: OptLanguageFields[String]
) extends Content {

  def supportedLanguages: Seq[String] =
    getSupportedLanguages(
      title,
      visualElement,
      introduction,
      metaDescription,
      tags,
      content,
      metaImage,
      disclaimer.getWithLanguageFields
    )
}

object Draft {
  implicit def eitherEnc: Encoder[Either[RelatedContentLink, Long]] = eitherEncoder[RelatedContentLink, Long]
  implicit def eitherDec: Decoder[Either[RelatedContentLink, Long]] = eitherDecoder[RelatedContentLink, Long]
  implicit val encoder: Encoder[Draft]                              = deriveEncoder
  implicit val decoder: Decoder[Draft]                              = deriveDecoder
}
