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
import no.ndla.network.model.NdlaRequest
import no.ndla.oembedproxy.Props
import no.ndla.oembedproxy.caching.{Memoize, MemoizeHelpers}
import no.ndla.oembedproxy.model.{DoNotUpdateMemoizeException, OEmbedEndpoint, OEmbedProvider}
import no.ndla.oembedproxy.service.OEmbedConverterService.{
  addYoutubeTimestampIfdefinedInRequest,
  handleYoutubeRequestUrl,
  removeQueryString,
  removeQueryStringAndFragment
}

import scala.util.{Failure, Success}
import sttp.client3.quick._

trait ProviderService {
  this: NdlaClient with Props with MemoizeHelpers =>
  val providerService: ProviderService

  class ProviderService extends StrictLogging {
    val NdlaFrontendEndpoint: OEmbedEndpoint =
      OEmbedEndpoint(
        Some(props.NdlaApprovedUrl),
        Some(props.NdlaFrontendOembedServiceUrl),
        None,
        None,
        None
      )

    val ListingFrontendEndpoint: OEmbedEndpoint =
      OEmbedEndpoint(
        Some(props.ListingFrontendApprovedUrls),
        Some(props.ListingFrontendOembedServiceUrl),
        None,
        None,
        None
      )

    val NdlaApiProvider: OEmbedProvider =
      OEmbedProvider(
        "NDLA Api",
        props.NdlaApiOembedProvider,
        List(NdlaFrontendEndpoint, ListingFrontendEndpoint)
      )

    val YoutubeEndpoint: OEmbedEndpoint = OEmbedEndpoint(
      Some(
        List(
          "http(s?)://*.youtube.com/watch*",
          "http(s?)://*.youtube.com/v/*",
          "http(s?)://youtu.be/*",
          "http(s?)://*.youtube.com/playlist?list=*",
          "http(s?)://youtube.com/playlist?list=*",
          "http(s?)://*.youtube.com/shorts*"
        )
      ),
      Some("https://www.youtube.com/oembed"),
      None,
      None,
      None
    )

    val YoutubeProvider: OEmbedProvider = OEmbedProvider(
      "YouTube",
      "https://www.youtube.com",
      List(YoutubeEndpoint),
      handleYoutubeRequestUrl,
      addYoutubeTimestampIfdefinedInRequest
    )

    val H5PApprovedUrls: List[String] = List(props.NdlaH5PApprovedUrl)

    val H5PEndpoint: OEmbedEndpoint =
      OEmbedEndpoint(Some(H5PApprovedUrls), Some(s"${props.NdlaH5POembedProvider}/oembed"), None, None, None)

    val H5PProvider: OEmbedProvider =
      OEmbedProvider("H5P", props.NdlaH5POembedProvider, List(H5PEndpoint))

    val TedApprovedUrls: List[String] = List(
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
      OEmbedEndpoint(Some(TedApprovedUrls), Some("https://www.ted.com/services/v1/oembed.json"), None, None, None)
    val TedProvider: OEmbedProvider = OEmbedProvider("Ted", "https://ted.com", List(TedEndpoint), removeQueryString)

    val IssuuApprovedUrls: List[String] = List("http://issuu.com/*", "https://issuu.com/*")

    val IssuuEndpoint: OEmbedEndpoint =
      OEmbedEndpoint(
        Some(IssuuApprovedUrls),
        Some("https://issuu.com/oembed"),
        None,
        None,
        Some(List(("iframe", "true")))
      )

    val IssuuProvider: OEmbedProvider =
      OEmbedProvider("Issuu", "https://issuu.com", List(IssuuEndpoint), removeQueryStringAndFragment)

    val loadProviders: Memoize[List[OEmbedProvider]] = Memoize(() => {
      logger.info("Provider cache was not found or out of date, fetching providers")
      _loadProviders()
    })

    def _loadProviders(): List[OEmbedProvider] = {
      val requestProviders = loadProvidersFromRequest(quickRequest.get(uri"${props.JSonProviderUrl}"))
      NdlaApiProvider :: TedProvider :: H5PProvider :: YoutubeProvider :: IssuuProvider :: requestProviders
    }

    def loadProvidersFromRequest(request: NdlaRequest): List[OEmbedProvider] = {
      val providersTry = ndlaClient.fetch[List[OEmbedProvider]](request)
      providersTry match {
        // Only keep providers with at least one endpoint with at least one url
        case Success(providers) =>
          providers
            .filter(_.endpoints.nonEmpty)
            .filter(_.endpoints.forall(endpoint => endpoint.url.isDefined))
        case Failure(ex) =>
          logger.error(s"Failed to load providers from ${request.uri}.")
          throw new DoNotUpdateMemoizeException(ex.getMessage)
      }
    }
  }
}
