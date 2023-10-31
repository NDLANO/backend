/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import no.ndla.network.model.{HttpRequestException, NdlaRequest}
import org.json4s.Formats

import scala.util.{Failure, Success}

class ImageApiClientTest extends UnitSuite with UnitTestEnvironment {

  override val imageApiClient = new ImageApiClient

  val DefaultImage: ImageMetaInformation =
    ImageMetaInformation("1", "http://api.test.ndla.no/images/1", "full", 1000, "contentType")

  test("That some metaInfo is returned when images is found") {
    when(
      ndlaClient.fetchWithForwardedAuth[ImageMetaInformation](any[NdlaRequest], any)(
        any[Manifest[ImageMetaInformation]],
        any[Formats]
      )
    )
      .thenReturn(Success(DefaultImage))

    val imageMeta = imageApiClient.imageMetaWithExternalId("abc", None)
    imageMeta.isDefined should be(true)
    imageMeta.get.id should equal("1")
    imageMeta.get.size should be(1000)
  }

  test("That none is returned when http 404 is received") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(true)
    when(
      ndlaClient.fetchWithForwardedAuth[ImageMetaInformation](any[NdlaRequest], any)(
        any[Manifest[ImageMetaInformation]],
        any[Formats]
      )
    )
      .thenReturn(Failure(exception))

    imageApiClient.imageMetaOnUrl("abc") should be(None)
  }

  test("That exception is returned when http-error") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(false)
    when(
      ndlaClient.fetchWithForwardedAuth[ImageMetaInformation](any[NdlaRequest], any)(
        any[Manifest[ImageMetaInformation]],
        any[Formats]
      )
    )
      .thenReturn(Failure(exception))

    intercept[HttpRequestException] {
      imageApiClient.imageMetaOnUrl("abc")
      fail("Exception should have been thrown")
    } should be(exception)

    verify(exception, times(1)).is404
  }

  test("That exception is returned for a randomly chosen exception") {
    val exception = mock[NoSuchElementException]
    when(
      ndlaClient.fetchWithForwardedAuth[ImageMetaInformation](any[NdlaRequest], any)(
        any[Manifest[ImageMetaInformation]],
        any[Formats]
      )
    )
      .thenReturn(Failure(exception))

    intercept[NoSuchElementException] {
      imageApiClient.imageMetaWithExternalId("abc", None)
      fail("Exception should have been thrown")
    } should be(exception)
  }
}
