/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description
import no.ndla.common.DeriveHelpers

@description("Information about article revision history")
case class ArticleRevisionHistoryDTO(
    @description("The revisions of an article, with the latest revision being the first in the list")
    revisions: Seq[ArticleDTO],
    @description("Whether or not the current revision is safe to delete")
    canDeleteCurrentRevision: Boolean
)

object ArticleRevisionHistoryDTO {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit val encoder: Encoder[ArticleRevisionHistoryDTO]          = deriveEncoder
  implicit val decoder: Decoder[ArticleRevisionHistoryDTO]          = deriveDecoder
  implicit def schema: sttp.tapir.Schema[ArticleRevisionHistoryDTO] = DeriveHelpers.getSchema
}
