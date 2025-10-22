/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description
import sttp.tapir.Schema
import no.ndla.common.DeriveHelpers

@description("Id for a single Article")
case class ContentIdDTO(
    @description("The unique id of the article")
    id: Long
)

object ContentIdDTO {
  implicit val encoder: Encoder[ContentIdDTO] = deriveEncoder
  implicit val decoder: Decoder[ContentIdDTO] = deriveDecoder
  implicit def schema: Schema[ContentIdDTO]   = DeriveHelpers.getSchema
}
