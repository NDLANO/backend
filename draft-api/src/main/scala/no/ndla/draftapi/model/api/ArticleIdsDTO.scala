/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import no.ndla.common.DeriveHelpers
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.generic.auto.*
import sttp.tapir.Schema

case class ArticleIdsDTO(articleId: Long, externalIds: List[String])

object ArticleIdsDTO {
  implicit val encoder: Encoder[ArticleIdsDTO] = deriveEncoder
  implicit val decoder: Decoder[ArticleIdsDTO] = deriveDecoder
  implicit def schema: Schema[ArticleIdsDTO]   = DeriveHelpers.getSchema
}
