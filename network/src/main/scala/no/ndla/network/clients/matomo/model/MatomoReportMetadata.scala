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

case class MatomoReportMetadata(name: String, parameters: Option[MatomoReportParameters])

object MatomoReportMetadata {
  implicit val decoder: Decoder[MatomoReportMetadata] = deriveDecoder
}

case class MatomoReportParameters(idDimension: Option[String])

object MatomoReportParameters {
  implicit val decoder: Decoder[MatomoReportParameters] = deriveDecoder
}
