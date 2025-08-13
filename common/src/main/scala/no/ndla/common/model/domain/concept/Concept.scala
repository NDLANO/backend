/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.concept

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.DraftCopyright
import no.ndla.common.model.domain.{Content, Responsible, Tag, Title}
import no.ndla.language.Language.getSupportedLanguages
import sttp.tapir.Schema

case class Concept(
    id: Option[Long],
    revision: Option[Int],
    title: Seq[Title],
    content: Seq[ConceptContent],
    copyright: Option[DraftCopyright],
    created: NDLADate,
    updated: NDLADate,
    updatedBy: Seq[String],
    tags: Seq[Tag],
    status: Status,
    visualElement: Seq[VisualElement],
    responsible: Option[Responsible],
    conceptType: ConceptType,
    glossData: Option[GlossData],
    editorNotes: Seq[ConceptEditorNote]
) extends Content {
  def supportedLanguages: Set[String] =
    getSupportedLanguages(title, content, tags, visualElement).toSet
}

object Concept {
  implicit val encoder: Encoder[Concept] = deriveEncoder
  implicit val decoder: Decoder[Concept] = deriveDecoder
  import sttp.tapir.generic.auto.*
  implicit def schema: Schema[Concept] = Schema.derivedSchema
}
