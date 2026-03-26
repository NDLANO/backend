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

case class MatomoPageUrlResult(label: String, nb_hits: Long, nb_visits: Long)

object MatomoPageUrlResult {
  implicit val decoder: Decoder[MatomoPageUrlResult] = deriveDecoder
}
