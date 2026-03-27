/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.frontpage

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class PopularArticle(pageUrl: String, contextId: Option[String], numHits: Long)

object PopularArticle {
  implicit val decoder: Decoder[PopularArticle] = deriveDecoder
  implicit val encoder: Encoder[PopularArticle] = deriveEncoder
}
