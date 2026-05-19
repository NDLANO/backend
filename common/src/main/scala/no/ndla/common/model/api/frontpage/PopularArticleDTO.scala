/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.frontpage

import io.circe.*
import io.circe.generic.semiauto.*
import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

case class PopularArticleDTO(contextId: String, numHits: Long)

object PopularArticleDTO {
  implicit def encoder: Encoder[PopularArticleDTO] = deriveEncoder[PopularArticleDTO]
  implicit def decoder: Decoder[PopularArticleDTO] = deriveDecoder[PopularArticleDTO]
  implicit def schema: Schema[PopularArticleDTO]   = Schema.derived[PopularArticleDTO]
}
