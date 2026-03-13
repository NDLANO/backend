/*
 * Part of NDLA article-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about article revision history")
case class ArticleRevisionHistoryDTO(
    @description("The revisions of an article, with the latest revision being the first in the list")
    revisions: Seq[ArticleV2DTO]
)

object ArticleRevisionHistoryDTO {

  implicit val encoder: Encoder[ArticleRevisionHistoryDTO] = deriveEncoder
  implicit val decoder: Decoder[ArticleRevisionHistoryDTO] = deriveDecoder
}
