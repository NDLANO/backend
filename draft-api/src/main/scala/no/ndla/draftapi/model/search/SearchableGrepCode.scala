/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class SearchableGrepCode(grepCode: String)

object SearchableGrepCode {
  implicit val encoder: Encoder[SearchableGrepCode] = deriveEncoder
  implicit val decoder: Decoder[SearchableGrepCode] = deriveDecoder
}
