/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients.matomo

import no.ndla.common.configuration.{BaseProps, Prop}

import scala.util.Properties.propOrElse

trait MatomoProps extends BaseProps {
  val MatomoUrl: Prop[String]            = prop("MATOMO_URL")
  val MatomoSiteId: Prop[String]         = prop("MATOMO_SITE_ID")
  val MatomoTokenAuth: Prop[String]      = prop("MATOMO_API_TOKEN")
  val MatomoSubjectDimensionName: String = propOrElse("MATOMO_SUBJECT_ID_DIMENSION_NAME", "subjectId")
}
