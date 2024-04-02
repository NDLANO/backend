/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.service

import no.ndla.network.NdlaClient
import no.ndla.oembedproxy.model.{OEmbed, OEmbedProvider, ProviderNotSupportedException}
import sttp.client3.quick._

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Try}
import scala.concurrent.duration.FiniteDuration

trait OEmbedServiceComponent {
  this: NdlaClient with ProviderService =>
  val oEmbedService: OEmbedService

  class OEmbedService(optionalProviders: Option[List[OEmbedProvider]] = None) {
    val remoteTimeout: FiniteDuration = 10.seconds

    private lazy val providers = optionalProviders.toList.flatten ++ providerService
      .loadProviders()
    private def getProvider(url: String): Option[OEmbedProvider] =
      providers.find(_.supports(url))

    private def fetchOembedFromProvider(
        provider: OEmbedProvider,
        url: String,
        maxWidth: Option[String],
        maxHeight: Option[String]
    ): Try[OEmbed] = {
      val uri = uri"${provider.requestUrl(url, maxWidth, maxHeight)}"
      ndlaClient.fetch[OEmbed](
        quickRequest
          .get(uri)
          .followRedirects(true)
          .readTimeout(remoteTimeout)
      )
    }

    def get(url: String, maxWidth: Option[String], maxHeight: Option[String]): Try[OEmbed] = {
      getProvider(url) match {
        case None =>
          Failure(ProviderNotSupportedException(s"Could not find an oembed-provider for the url '$url'"))
        case Some(provider) =>
          fetchOembedFromProvider(provider, url, maxWidth, maxHeight).map(provider.postProcessor(url, _))
      }
    }

  }
}
