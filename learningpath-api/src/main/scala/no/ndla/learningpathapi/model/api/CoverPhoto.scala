/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class CoverPhoto(url: String, metaUrl: String)

object CoverPhoto {
  implicit val encoder: Encoder[CoverPhoto] = deriveEncoder
  implicit val decoder: Decoder[CoverPhoto] = deriveDecoder
}
