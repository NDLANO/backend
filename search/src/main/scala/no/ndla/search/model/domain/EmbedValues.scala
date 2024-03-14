/*
 * Part of NDLA search
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.search.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class EmbedValues(
    id: List[String],
    resource: Option[String],
    language: String
)

object EmbedValues {
  implicit val encoder: Encoder[EmbedValues] = deriveEncoder
  implicit val decoder: Decoder[EmbedValues] = deriveDecoder
}
