/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class CoverPhoto(imageId: String, altText: String)

object CoverPhoto {
  implicit val encoder: Encoder[CoverPhoto] = deriveEncoder
  implicit val decoder: Decoder[CoverPhoto] = deriveDecoder
}
