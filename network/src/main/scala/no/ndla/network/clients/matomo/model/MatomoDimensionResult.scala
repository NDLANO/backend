/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients.matomo.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class MatomoDimensionResult(label: String, nb_hits: Long, nb_visits: Long, idsubdatatable: Option[Long])

object MatomoDimensionResult {
  implicit val decoder: Decoder[MatomoDimensionResult] = deriveDecoder
}
