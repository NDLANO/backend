/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.article

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

@description("Partial publish article dto")
case class PartialPublishArticlesBulkDTO(
    @description("Map of article id to partial publish article dto")
    idTo: Map[Long, PartialPublishArticleDTO]
)

object PartialPublishArticlesBulkDTO {
  implicit val encoder: Encoder[PartialPublishArticlesBulkDTO] = deriveEncoder
  implicit val decoder: Decoder[PartialPublishArticlesBulkDTO] = deriveDecoder
  implicit def schema: Schema[PartialPublishArticlesBulkDTO]   = Schema.any[PartialPublishArticlesBulkDTO]
}
