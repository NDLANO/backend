/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate

import java.util.UUID

case class Comment(id: UUID, created: NDLADate, updated: NDLADate, content: String, isOpen: Boolean, solved: Boolean)

object Comment {
  implicit val encoder: Encoder[Comment] = deriveEncoder
  implicit val decoder: Decoder[Comment] = deriveDecoder
}
