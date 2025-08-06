/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.service

import no.ndla.network.model.{HttpRequestException, NdlaRequest}
import no.ndla.oembedproxy.caching.Memoize
import no.ndla.oembedproxy.model.*
import no.ndla.oembedproxy.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.TryValues.*

import scala.util.{Failure, Success}

class OEmbedServiceTest extends UnitSuite with TestEnvironment {

  val ndlaProvider: OEmbedProvider = OEmbedProvider(
    "ndla",
    "https://ndla.no",
    List(OEmbedEndpoint(Some(List("https://ndla.no/*")), "https://ndla.no/oembed", None, None, None))
  )

  val youtubeProvider: OEmbedProvider = OEmbedProvider(
    "YouTube",
    "https://www.youtube.com/",
    List(
      OEmbedEndpoint(
        Some(List("https://www.youtube.com/*")),
        "https://www.youtube.com/oembed",
        Some(true),
        None,
        None
      )
    )
  )

  val OEmbedResponse: OEmbedDTO = OEmbedDTO(
    "rich",
    "1.0",
    Some("A Confectioner in the UK"),
    None,
    None,
    None,
    Some("NDLA - Nasjonal digital læringsarene"),
    Some("http://ndla.no"),
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    Some("<iframe src='http://ndla.no/en/node/128905/oembed' allowfullscreen></iframe>")
  )

  override lazy val oEmbedService                    = new OEmbedService(Some(List(ndlaProvider, youtubeProvider)))
  val providerMemoize                                = new Memoize(0, 0, () => List[OEmbedProvider](), false)
  override lazy val providerService: ProviderService = new ProviderService {
    override val loadProviders: Memoize[List[OEmbedProvider]] = providerMemoize
  }

  test("That get returns Failure(ProviderNotSupportedException) when no providers support the url") {
    val ex = oEmbedService.get(url = "ABC", None, None)
    ex should be(Failure(ProviderNotSupportedException("Could not find an oembed-provider for the url 'ABC'")))
  }

  test("That get returns a failure with HttpRequestException when receiving http error") {
    when(ndlaClient.fetch[OEmbedDTO](any[NdlaRequest])(using any))
      .thenReturn(Failure(new HttpRequestException("An error occured")))
    val oembedTry = oEmbedService.get("https://www.youtube.com/abc", None, None)
    oembedTry.isFailure should be(true)
    oembedTry.failure.exception.getMessage should equal("An error occured")
  }

  test("That get returns a Success with an oEmbed when http call is successful") {
    when(ndlaClient.fetch[OEmbedDTO](any[NdlaRequest])(using any))
      .thenReturn(Success(OEmbedResponse))
    val oembedTry = oEmbedService.get("https://ndla.no/abc", None, None)
    oembedTry.isSuccess should be(true)
    oembedTry.get.`type` should equal("rich")
    oembedTry.get.title.getOrElse("") should equal("A Confectioner in the UK")
  }

}
