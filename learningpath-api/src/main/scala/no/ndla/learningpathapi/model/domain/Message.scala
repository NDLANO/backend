/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate

case class Message(message: String, adminName: String, date: NDLADate)

object Message {
  implicit val encoder: Encoder[Message] = deriveEncoder
  implicit val decoder: Decoder[Message] = deriveDecoder
}
