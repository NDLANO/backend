/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.service

import no.ndla.network.model.{HttpRequestException, NdlaRequest}
import no.ndla.oembedproxy.model.*
import no.ndla.oembedproxy.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import sttp.client3.quick.*

import scala.util.{Failure, Success}

class ProviderServiceTest extends UnitSuite with TestEnvironment {

  val IncompleteProvider: OEmbedProvider = OEmbedProvider(
    "gfycat",
    "https://gfycat.com",
    List(OEmbedEndpoint(Some(List("http://gfycat.com/*")), None, None, None))
  )

  val CompleteProvider: OEmbedProvider = OEmbedProvider(
    "IFTTT",
    "http://www.ifttt.com",
    List(
      OEmbedEndpoint(Some(List("http://ifttt.com/recipes/*")), Some("http://www.ifttt.com/oembed/"), Some(true), None)
    )
  )

  override val providerService = new ProviderService

  test("That loadProvidersFromRequest fails on invalid url/bad response") {
    val invalidUrl = "invalidUrl123"
    when(ndlaClient.fetch[OEmbed](any[NdlaRequest])(any, any[Manifest[OEmbed]]))
      .thenReturn(Failure(new HttpRequestException("An error occured")))
    intercept[DoNotUpdateMemoizeException] {
      providerService.loadProvidersFromRequest(quickRequest.get(uri"$invalidUrl"))
    }
  }

  test("That loadProvidersFromRequest does not return an incomplete provider") {
    when(ndlaClient.fetch[List[OEmbedProvider]](any[NdlaRequest])(any, any[Manifest[List[OEmbedProvider]]]))
      .thenReturn(Success(List(IncompleteProvider)))

    val providers = providerService.loadProvidersFromRequest(mock[NdlaRequest])
    providers.size should be(0)
  }

  test("That loadProvidersFromRequest works for a single provider") {
    when(ndlaClient.fetch[List[OEmbedProvider]](any[NdlaRequest])(any, any[Manifest[List[OEmbedProvider]]]))
      .thenReturn(Success(List(CompleteProvider)))

    val providers = providerService.loadProvidersFromRequest(mock[NdlaRequest])
    providers.size should be(1)
  }

  test("That loadProvidersFromRequest only returns the complete provider") {
    when(ndlaClient.fetch[List[OEmbedProvider]](any[NdlaRequest])(any, any[Manifest[List[OEmbedProvider]]]))
      .thenReturn(Success(List(IncompleteProvider, CompleteProvider)))

    val providers = providerService.loadProvidersFromRequest(mock[NdlaRequest])
    providers.size should be(1)
  }
}
