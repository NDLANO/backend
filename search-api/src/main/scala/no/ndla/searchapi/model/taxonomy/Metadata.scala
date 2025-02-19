/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.taxonomy

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class Metadata(grepCodes: Seq[String], visible: Boolean, customFields: Map[String, String])

object Metadata {
  implicit val encoder: Encoder[Metadata] = deriveEncoder
  implicit val decoder: Decoder[Metadata] = deriveDecoder
}
