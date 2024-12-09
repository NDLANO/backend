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
import sttp.tapir.Schema.annotations.description

@description("Representation of an embeddable url")
case class EmbedUrlV2DTO(
    @description("The url") url: String,
    @description("Type of embed content") embedType: String
)

object EmbedUrlV2DTO {
  implicit val encoder: Encoder[EmbedUrlV2DTO] = deriveEncoder
  implicit val decoder: Decoder[EmbedUrlV2DTO] = deriveDecoder
}
