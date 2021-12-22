/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.network.{AuthUser, Domains}
import scala.util.Properties.envOrNone

object OEmbedProxyProperties {

  val ApplicationPort: Int = propOrElse("APPLICATION_PORT", "80").toInt

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val Environment: String = propOrElse("NDLA_ENVIRONMENT", "local")

  val Auth0LoginEndpoint =
    s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val ContactEmail = "support+api@ndla.no"
  val JSonProviderUrl = "https://oembed.com/providers.json"
  val ProviderListCacheAgeInMs: Long = 1000 * 60 * 60 * 24 // 24 hour caching
  val ProviderListRetryTimeInMs: Long = 1000 * 60 * 60 // 1 hour before retrying a failed attempt.

  val NdlaFrontendOembedServiceUrl: String = Map(
    "local" -> "http://ndla-frontend.ndla-local/oembed",
    "prod" -> "https://ndla.no/oembed"
  ).getOrElse(Environment, s"https://$Environment.ndla.no/oembed")

  val ListingFrontendOembedServiceUrl: String = Map(
    "local" -> "http://listing-frontend.ndla-local/oembed",
    "prod" -> "https://liste.ndla.no/oembed"
  ).getOrElse(Environment, s"https://liste.$Environment.ndla.no/oembed")

  val ListingFrontendApprovedUrls: List[String] = Map(
    "local" -> List("http://localhost:30020/*", "http://listing-frontend.ndla-local/*"),
    "prod" -> List("https?://liste.ndla.no/*")
  ).getOrElse(Environment, List(s"https?://liste.$Environment.ndla.no/*"))

  val NdlaApiOembedProvider: String = Domain

  val NdlaApprovedUrl: List[String] = Map(
    "local" -> List("http://localhost:30017/*", "http://ndla-frontend.ndla-local/*"),
    "prod" -> List("https?://www.ndla.no/*", "https?://ndla.no/*", "https?://beta.ndla.no/*")
  ).getOrElse(Environment, List(s"https?://ndla-frontend.$Environment.ndla.no/*", s"https?://$Environment.ndla.no/*"))

  val NdlaH5POembedProvider: String = Map(
    "staging" -> "https://h5p-staging.ndla.no",
    "prod" -> "https://h5p.ndla.no",
  ).getOrElse(Environment, "https://h5p-test.ndla.no")

  val NdlaH5PApprovedUrl: String = Map(
    "staging" -> "https://h5p-staging.ndla.no/resource/*",
    "prod" -> "https://h5p.ndla.no/resource/*",
  ).getOrElse(Environment, "https://h5p-test.ndla.no/resource/*")

  val OembedProxyControllerMountPoint = "/oembed-proxy/v1/oembed"
  val ResourcesAppMountPoint = "/oembed-proxy/api-docs"
  val HealthControllerMountPoint = "/health"

  lazy val Domain: String = Domains.get(Environment)

  def propOrElse(key: String, default: => String): String = {
    envOrNone(key) match {
      case Some(env) => env
      case None      => default
    }
  }
}
