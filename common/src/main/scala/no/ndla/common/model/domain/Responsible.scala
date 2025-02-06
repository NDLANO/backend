/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate

case class Responsible(
    responsibleId: String,
    lastUpdated: NDLADate
)

object Responsible {
  implicit val encoder: Encoder[Responsible] = deriveEncoder
  implicit val decoder: Decoder[Responsible] = deriveDecoder
}
