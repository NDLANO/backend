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
import no.ndla.oembedproxy.OEmbedProxyProperties
import no.ndla.oembedproxy.caching.Memoize
import no.ndla.oembedproxy.model.{DoNotUpdateMemoizeException, OEmbedEndpoint, OEmbedProvider}
import no.ndla.oembedproxy.service.OEmbedConverterService.{
  addYoutubeTimestampIfdefinedInRequest,
  removeQueryString,
  removeQueryStringAndFragment,
  handleYoutubeRequestUrl
}
import org.json4s.DefaultFormats

import scala.util.{Failure, Success}
import scalaj.http.{Http, HttpRequest}

trait ProviderService {
  this: NdlaClient =>
  val providerService: ProviderService

  class ProviderService extends LazyLogging {
    implicit val formats: DefaultFormats = org.json4s.DefaultFormats

    val NdlaApiApprovedUrls: List[String] = OEmbedProxyProperties.NdlaApprovedUrl

    val NdlaApiEndpoint: OEmbedEndpoint =
      OEmbedEndpoint(Some(NdlaApiApprovedUrls), Some(OEmbedProxyProperties.NdlaApiOembedServiceUrl), None, None)

    val ListingFrontendEndpoint: OEmbedEndpoint =
      OEmbedEndpoint(Some(OEmbedProxyProperties.ListingFrontendApprovedUrls),
                     Some(OEmbedProxyProperties.ListingFrontendOembedServiceUrl),
                     None,
                     None)

    val NdlaApiProvider: OEmbedProvider =
      OEmbedProvider("NDLA Api",
                     OEmbedProxyProperties.NdlaApiOembedProvider,
                     List(NdlaApiEndpoint, ListingFrontendEndpoint),
                     removeQueryString)

    val YoutubeEndpoint: OEmbedEndpoint =
      OEmbedEndpoint(None, Some("https://www.youtube.com/oembed"), None, None)

    val YoutuProvider: OEmbedProvider = OEmbedProvider("YouTube",
                                                       "https://youtu.be",
                                                       List(YoutubeEndpoint),
                                                       handleYoutubeRequestUrl,
                                                       addYoutubeTimestampIfdefinedInRequest)

    val YoutubeProvider: OEmbedProvider = OEmbedProvider("YouTube",
                                                         "https://www.youtube.com",
                                                         List(YoutubeEndpoint),
                                                         handleYoutubeRequestUrl,
                                                         addYoutubeTimestampIfdefinedInRequest)

    val H5PApprovedUrls = List(OEmbedProxyProperties.NdlaH5PApprovedUrl)

    val H5PEndpoint: OEmbedEndpoint =
      OEmbedEndpoint(Some(H5PApprovedUrls), Some(s"${OEmbedProxyProperties.NdlaH5POembedProvider}/oembed"), None, None)

    val H5PProvider: OEmbedProvider =
      OEmbedProvider("H5P", OEmbedProxyProperties.NdlaH5POembedProvider, List(H5PEndpoint))

    val TedApprovedUrls = List(
      "https://www.ted.com/talks/*",
      "http://www.ted.com/talks/*",
      "https://ted.com/talks/*",
      "http://ted.com/talks/*",
      "www.ted.com/talks/*",
      "ted.com/talks/*",
      "https://www.embed.ted.com/talks/*",
      "http://www.embed.ted.com/talks/*",
      "https://embed.ted.com/talks/*",
      "http://embed.ted.com/talks/*",
      "www.embed.ted.com/talks/*",
      "embed.ted.com/talks/*"
    )

    val TedEndpoint: OEmbedEndpoint =
      OEmbedEndpoint(Some(TedApprovedUrls), Some("https://www.ted.com/services/v1/oembed.json"), None, None)
    val TedProvider: OEmbedProvider = OEmbedProvider("Ted", "https://ted.com", List(TedEndpoint), removeQueryString)

    val IssuuApprovedUrls = List("http://issuu.com/*", "https://issuu.com/*")

    val IssuuEndpoint: OEmbedEndpoint =
      OEmbedEndpoint(Some(IssuuApprovedUrls), Some("https://issuu.com/oembed"), None, None, List(("iframe", "true")))

    val IssuuProvider: OEmbedProvider =
      OEmbedProvider("Issuu", "https://issuu.com", List(IssuuEndpoint), removeQueryStringAndFragment)

    val loadProviders: Memoize[List[OEmbedProvider]] = Memoize(() => {
      logger.info("Provider cache was not found or out of date, fetching providers")
      _loadProviders()
    })

    def _loadProviders(): List[OEmbedProvider] = {
      NdlaApiProvider :: TedProvider :: H5PProvider :: YoutubeProvider :: YoutuProvider :: IssuuProvider :: loadProvidersFromRequest(
        Http(OEmbedProxyProperties.JSonProviderUrl))
    }

    def loadProvidersFromRequest(request: HttpRequest): List[OEmbedProvider] = {
      val providersTry = ndlaClient.fetch[List[OEmbedProvider]](request)
      providersTry match {
        // Only keep providers with at least one endpoint with at least one url
        case Success(providers) =>
          providers
            .filter(_.endpoints.nonEmpty)
            .filter(_.endpoints.forall(endpoint => endpoint.url.isDefined))
        case Failure(ex) =>
          logger.error(s"Failed to load providers from ${request.url}.")
          throw new DoNotUpdateMemoizeException(ex.getMessage)
      }
    }
  }
}
