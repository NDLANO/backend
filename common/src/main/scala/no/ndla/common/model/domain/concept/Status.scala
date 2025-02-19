/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.concept

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class Status(
    current: ConceptStatus,
    other: Set[ConceptStatus]
)

object Status {
  implicit val encoder: Encoder[Status] = deriveEncoder
  implicit val decoder: Decoder[Status] = deriveDecoder

  def default: Status = {
    Status(
      current = ConceptStatus.IN_PROGRESS,
      other = Set.empty
    )
  }

}
