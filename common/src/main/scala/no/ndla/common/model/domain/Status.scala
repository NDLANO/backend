/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.draft.DraftStatus

case class Status(current: DraftStatus, other: Set[DraftStatus])

object Status {
  implicit val encoder: Encoder[Status] = deriveEncoder
  implicit val decoder: Decoder[Status] = deriveDecoder
}
