/*
 * Part of NDLA draft-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.domain

import no.ndla.common.DeriveHelpers

case class ArticleIds(articleId: Long, externalId: List[String], importId: Option[String] = None)

object ArticleIds {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}
  import sttp.tapir.generic.auto.*
  import sttp.tapir.Schema

  implicit val encoder: Encoder[ArticleIds] = deriveEncoder
  implicit val decoder: Decoder[ArticleIds] = deriveDecoder
  implicit def schema: Schema[ArticleIds]   = DeriveHelpers.getSchema
}
