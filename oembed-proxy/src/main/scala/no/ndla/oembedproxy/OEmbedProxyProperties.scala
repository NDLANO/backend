/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.common.configuration.BaseProps
import no.ndla.network.{AuthUser, Domains}

import scala.util.Properties.{propOrElse, propOrNone}

type Props = OEmbedProxyProperties

class OEmbedProxyProperties extends BaseProps {
  def ApplicationName: String = "oembed-proxy"
  val ApplicationPort: Int    = propOrElse("APPLICATION_PORT", "80").toInt

  val Auth0LoginEndpoint: String = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val JSonProviderUrl                 = "https://oembed.com/providers.json"
  val ProviderListCacheAgeInMs: Long  = 1000 * 60 * 60 * 24 // 24 hour caching
  val ProviderListRetryTimeInMs: Long = 1000 * 60 * 60      // 1 hour before retrying a failed attempt.

  val NdlaFrontendUrl: Option[String] = propOrNone("NDLA_FRONTEND_HOST")

  val NdlaFrontendOembedServiceUrlFallback: String = Map(
    "local" -> "http://ndla-frontend.ndla-local/oembed",
    "prod"  -> "https://ndla.no/oembed",
  ).getOrElse(Environment, s"https://$Environment.ndla.no/oembed")

  val NdlaFrontendOembedServiceUrl: String = NdlaFrontendUrl
    .map { url =>
      val NdlaFrontendSchema: String = propOrElse("NDLA_FRONTEND_SCHEMA", "http")
      s"$NdlaFrontendSchema://$url/oembed"
    }
    .getOrElse(NdlaFrontendOembedServiceUrlFallback)

  val NdlaApiOembedProvider: String = Domain

  val NdlaApprovedUrl: List[String] = Map(
    "local" -> List("http://localhost:3000/*", "http://localhost:30017/*", "http://ndla-frontend.ndla-local/*"),
    "prod"  -> List("https?://www.ndla.no/*", "https?://ndla.no/*", "https?://beta.ndla.no/*"),
  ).getOrElse(Environment, List(s"https?://ndla-frontend.$Environment.ndla.no/*", s"https?://$Environment.ndla.no/*"))

  val NdlaH5POembedProvider: String = Map("staging" -> "https://h5p-staging.ndla.no", "prod" -> "https://h5p.ndla.no")
    .getOrElse(Environment, "https://h5p-test.ndla.no")

  val NdlaH5PApprovedUrl: String = Map(
    "staging" -> "https://h5p-staging.ndla.no/resource/*",
    "prod"    -> "https://h5p.ndla.no/resource/*",
  ).getOrElse(Environment, "https://h5p-test.ndla.no/resource/*")

  val OembedProxyControllerMountPoint = "/oembed-proxy/v1/oembed"
  val ResourcesAppMountPoint          = "/oembed-proxy/api-docs"
  val HealthControllerMountPoint      = "/health"

  private lazy val Domain: String = Domains.get(Environment)
}
