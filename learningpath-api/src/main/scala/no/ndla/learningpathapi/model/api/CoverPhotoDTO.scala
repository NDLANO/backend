/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class CoverPhotoDTO(url: String, metaUrl: String)

object CoverPhotoDTO {
  implicit val encoder: Encoder[CoverPhotoDTO] = deriveEncoder
  implicit val decoder: Decoder[CoverPhotoDTO] = deriveDecoder
}
