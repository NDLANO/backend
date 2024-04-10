/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.concept

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.DraftCopyright
import no.ndla.common.model.domain.{Content, Responsible, Tag, Title}
import no.ndla.language.Language.getSupportedLanguages

case class Concept(
    id: Option[Long],
    revision: Option[Int],
    title: Seq[Title],
    content: Seq[ConceptContent],
    copyright: Option[DraftCopyright],
    created: NDLADate,
    updated: NDLADate,
    updatedBy: Seq[String],
    metaImage: Seq[ConceptMetaImage],
    tags: Seq[Tag],
    subjectIds: Set[String],
    articleIds: Seq[Long],
    status: Status,
    visualElement: Seq[VisualElement],
    responsible: Option[Responsible],
    conceptType: ConceptType.Value,
    glossData: Option[GlossData],
    editorNotes: Seq[EditorNote]
) extends Content {
  def supportedLanguages: Set[String] =
    getSupportedLanguages(title, content, tags, visualElement, metaImage).toSet
}

object Concept {
  implicit val encoder: Encoder[Concept] = deriveEncoder
  implicit val decoder: Decoder[Concept] = deriveDecoder
}
