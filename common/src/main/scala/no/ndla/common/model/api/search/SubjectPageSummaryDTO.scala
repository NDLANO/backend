/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.common.model.api.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("DTO for subject page summary in search results")
case class SubjectPageSummaryDTO(
    id: Long,
    name: String,
    metaDescription: MetaDescriptionDTO
)

object SubjectPageSummaryDTO {
  implicit val encoder: Encoder[SubjectPageSummaryDTO] = deriveEncoder
  implicit val decoder: Decoder[SubjectPageSummaryDTO] = deriveDecoder
}
