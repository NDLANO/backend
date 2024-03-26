/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.grep

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class GrepTitle(spraak: String, verdi: String)

object GrepTitle {
  implicit val encoder: Encoder[GrepTitle] = deriveEncoder
  implicit val decoder: Decoder[GrepTitle] = deriveDecoder
}
