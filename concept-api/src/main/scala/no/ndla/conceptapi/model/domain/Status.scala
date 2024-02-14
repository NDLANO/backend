/*
 * Part of NDLA concept-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class Status(
    current: ConceptStatus,
    other: Set[ConceptStatus]
)

object Status {
  implicit val encoder: Encoder[Status] = deriveEncoder
  implicit val decoder: Decoder[Status] = deriveDecoder

  def default = {
    Status(
      current = ConceptStatus.IN_PROGRESS,
      other = Set.empty
    )
  }

}
