/*
 * Part of NDLA common
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate

case class EditorNote(note: String, user: String, status: Status, timestamp: NDLADate)

object EditorNote {
  implicit val encoder: Encoder[EditorNote] = deriveEncoder
  implicit val decoder: Decoder[EditorNote] = deriveDecoder
}
