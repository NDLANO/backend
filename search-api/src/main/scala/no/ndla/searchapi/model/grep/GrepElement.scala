/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.grep

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class GrepElement(kode: String, tittel: Seq[GrepTitle])

object GrepElement {
  implicit val encoder: Encoder[GrepElement] = deriveEncoder
  implicit val decoder: Decoder[GrepElement] = deriveDecoder
}
