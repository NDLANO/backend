/*
 * Part of NDLA draft-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.draft.DraftStatus

case class SearchableStatus(current: DraftStatus, other: Set[DraftStatus])

object SearchableStatus {
  implicit val encoder: Encoder[SearchableStatus] = deriveEncoder
  implicit val decoder: Decoder[SearchableStatus] = deriveDecoder
}
