/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}

case class PopularArticlesResultDTO(subjectId: Long, articleCount: Int)

object PopularArticlesResultDTO {
  implicit def encoder: Encoder.AsObject[PopularArticlesResultDTO] = deriveEncoder[PopularArticlesResultDTO]
  implicit def decoder: Decoder[PopularArticlesResultDTO]          = deriveDecoder[PopularArticlesResultDTO]
}
