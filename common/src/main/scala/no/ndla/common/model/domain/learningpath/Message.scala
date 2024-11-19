/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.learningpath

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate

case class Message(message: String, adminName: String, date: NDLADate)

object Message {
  implicit val encoder: Encoder[Message] = deriveEncoder
  implicit val decoder: Decoder[Message] = deriveDecoder
}
