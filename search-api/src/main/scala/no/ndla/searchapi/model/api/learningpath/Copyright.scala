/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.learningpath

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.{Author, License}
import sttp.tapir.Schema.annotations.description

@description("Description of copyright information")
case class Copyright(
    @description("Describes the license of the learningpath") license: License,
    @description("List of authors") contributors: Seq[Author]
)

object Copyright {
  implicit val encoder: Encoder[Copyright] = deriveEncoder
  implicit val decoder: Decoder[Copyright] = deriveDecoder
}
