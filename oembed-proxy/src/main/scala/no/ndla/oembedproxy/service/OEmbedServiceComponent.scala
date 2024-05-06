/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import no.ndla.oembedproxy.model.{OEmbed, OEmbedProvider, ProviderNotSupportedException}
import sttp.client3.quick.*

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.FiniteDuration

trait OEmbedServiceComponent {
  this: NdlaClient with ProviderService =>
  val oEmbedService: OEmbedService

  class OEmbedService(optionalProviders: Option[List[OEmbedProvider]] = None) extends StrictLogging {
    val remoteTimeout: FiniteDuration = 10.seconds

    private lazy val providers = optionalProviders.toList.flatten ++ providerService
      .loadProviders()
    private def getProvider(url: String): Option[OEmbedProvider] =
      providers.find(_.supports(url))

    private val MaxFetchOembedRetries: Int = 3
    @tailrec
    private def fetchOembedFromProvider(
        provider: OEmbedProvider,
        url: String,
        maxWidth: Option[String],
        maxHeight: Option[String],
        retryCount: Int
    ): Try[OEmbed] = {
      val uri = uri"${provider.requestUrl(url, maxWidth, maxHeight)}"
      ndlaClient.fetch[OEmbed](
        quickRequest
          .get(uri)
          .followRedirects(true)
          .readTimeout(remoteTimeout)
      ) match {
        case Success(oembed)                   => Success(oembed)
        case Failure(ex: HttpRequestException) => Failure(ex)
        case Failure(ex) if retryCount < MaxFetchOembedRetries =>
          logger.error(
            s"Failed to fetch oembed from provider ${provider.providerName} for url $url. Retrying ${retryCount + 1}/$MaxFetchOembedRetries.",
            ex
          )
          fetchOembedFromProvider(
            provider,
            url,
            maxWidth,
            maxHeight,
            retryCount + 1
          )
        case Failure(ex) => Failure(ex)
      }
    }

    def get(url: String, maxWidth: Option[String], maxHeight: Option[String]): Try[OEmbed] = {
      getProvider(url) match {
        case None =>
          Failure(ProviderNotSupportedException(s"Could not find an oembed-provider for the url '$url'"))
        case Some(provider) =>
          fetchOembedFromProvider(provider, url, maxWidth, maxHeight, 0).map(provider.postProcessor(url, _))
      }
    }

  }
}
