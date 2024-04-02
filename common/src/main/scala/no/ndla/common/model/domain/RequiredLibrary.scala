/*
 * Part of NDLA common
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class RequiredLibrary(mediaType: String, name: String, url: String)

object RequiredLibrary {
  implicit def encoder: Encoder[RequiredLibrary] = deriveEncoder[RequiredLibrary]
  implicit def decoder: Decoder[RequiredLibrary] = deriveDecoder[RequiredLibrary]
}
