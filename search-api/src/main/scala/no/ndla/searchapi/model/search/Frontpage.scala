/*
 * Part of NDLA search-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.frontpage.SubjectPage

case class Frontpage(
    id: Long,
    name: String,
    domainObject: SubjectPage
)

object Frontpage {
  implicit val encoder: Encoder[Frontpage] = deriveEncoder
  implicit val decoder: Decoder[Frontpage] = deriveDecoder
}
