/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.frontpage

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class VisualElementDTO(`type`: String, url: String, alt: Option[String])

object VisualElementDTO {
  implicit val encoder: Encoder[VisualElementDTO] = deriveEncoder
  implicit val decoder: Decoder[VisualElementDTO] = deriveDecoder
}
