/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description
import no.ndla.common.DeriveHelpers

@description("Single failed result")
case class PartialPublishFailureDTO(
    @description("Id of the article in question")
    id: Long,
    @description("Error message")
    message: String,
)

object PartialPublishFailureDTO {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit def encoder: Encoder[PartialPublishFailureDTO] = deriveEncoder
  implicit def decoder: Decoder[PartialPublishFailureDTO] = deriveDecoder
  implicit def schema: Schema[PartialPublishFailureDTO]   = DeriveHelpers.getSchema[PartialPublishFailureDTO]
}

@description("A list of articles that were partial published to article-api")
case class MultiPartialPublishResultDTO(
    @description("Successful ids")
    successes: Seq[Long],
    @description("Failed ids with error messages")
    failures: Seq[PartialPublishFailureDTO],
)

object MultiPartialPublishResultDTO {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit def encoder: Encoder[MultiPartialPublishResultDTO] = deriveEncoder
  implicit def decoder: Decoder[MultiPartialPublishResultDTO] = deriveDecoder
  implicit def schema: Schema[MultiPartialPublishResultDTO]   = DeriveHelpers.getSchema[MultiPartialPublishResultDTO]
}
