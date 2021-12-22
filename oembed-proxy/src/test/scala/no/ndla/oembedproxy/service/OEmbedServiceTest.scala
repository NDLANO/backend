/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.service

import no.ndla.network.model.HttpRequestException
import no.ndla.oembedproxy.caching.Memoize
import no.ndla.oembedproxy.model._
import no.ndla.oembedproxy.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.TryValues._

import scala.util.{Failure, Success}
import scalaj.http.HttpRequest

class OEmbedServiceTest extends UnitSuite with TestEnvironment {

  val ndlaProvider: OEmbedProvider = OEmbedProvider(
    "ndla",
    "https://ndla.no",
    List(OEmbedEndpoint(Some(List("https://ndla.no/*")), Some("https://ndla.no/oembed"), None, None)))

  val youtubeProvider: OEmbedProvider = OEmbedProvider(
    "YouTube",
    "https://www.youtube.com/",
    List(
      OEmbedEndpoint(Some(List("https://www.youtube.com/*")), Some("https://www.youtube.com/oembed"), Some(true), None))
  )

  val OEmbedResponse: OEmbed = OEmbed(
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

  override val oEmbedService = new OEmbedService(Some(List(ndlaProvider, youtubeProvider)))
  val providerMemoize = new Memoize(0, 0, () => List[OEmbedProvider](), false)
  override val providerService: ProviderService = new ProviderService {
    override val loadProviders: Memoize[List[OEmbedProvider]] = providerMemoize
  }

  test("That get returns Failure(ProviderNotSupportedException) when no providers support the url") {
    val Failure(ex: ProviderNotSupportedException) =
      oEmbedService.get(url = "ABC", None, None)

    ex.getMessage should equal("Could not find an oembed-provider for the url 'ABC'")
  }

  test("That get returns a failure with HttpRequestException when receiving http error") {
    when(ndlaClient.fetch[OEmbed](any[HttpRequest])(any[Manifest[OEmbed]]))
      .thenReturn(Failure(new HttpRequestException("An error occured")))
    val oembedTry = oEmbedService.get("https://www.youtube.com/abc", None, None)
    oembedTry.isFailure should be(true)
    oembedTry.failure.exception.getMessage should equal("An error occured")
  }

  test("That get returns a Success with an oEmbed when http call is successful") {
    when(ndlaClient.fetch[OEmbed](any[HttpRequest])(any[Manifest[OEmbed]]))
      .thenReturn(Success(OEmbedResponse))
    val oembedTry = oEmbedService.get("https://ndla.no/abc", None, None)
    oembedTry.isSuccess should be(true)
    oembedTry.get.`type` should equal("rich")
    oembedTry.get.title.getOrElse("") should equal("A Confectioner in the UK")
  }

}
