/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.NdlaClient
import no.ndla.oembedproxy.model.{OEmbed, OEmbedProvider, ProviderNotSupportedException}
import org.json4s.DefaultFormats

import scala.util.{Failure, Try}
import scalaj.http.{Http, HttpOptions}

trait OEmbedServiceComponent extends LazyLogging {
  this: NdlaClient with ProviderService =>
  val oEmbedService: OEmbedService

  class OEmbedService(optionalProviders: Option[List[OEmbedProvider]] = None) {
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

    val remoteTimeout: Int = 10 * 1000 // 10 Seconds

    private lazy val providers = optionalProviders.toList.flatten ++ providerService
      .loadProviders()
    private def getProvider(url: String): Option[OEmbedProvider] =
      providers.find(_.supports(url))

    private def fetchOembedFromProvider(provider: OEmbedProvider,
                                        url: String,
                                        maxWidth: Option[String],
                                        maxHeight: Option[String]): Try[OEmbed] = {
      ndlaClient.fetch[OEmbed](
        Http(provider.requestUrl(url, maxWidth, maxHeight))
          .option(HttpOptions.followRedirects(true))
          .timeout(remoteTimeout, remoteTimeout)
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
