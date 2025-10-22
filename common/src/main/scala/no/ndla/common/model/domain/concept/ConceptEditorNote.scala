/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.concept

import no.ndla.common.model.NDLADate
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class ConceptEditorNote(note: String, user: String, status: Status, timestamp: NDLADate)

object ConceptEditorNote {
  implicit val encoder: Encoder[ConceptEditorNote] = deriveEncoder
  implicit val decoder: Decoder[ConceptEditorNote] = deriveDecoder
}
