package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.model.domain.HttpRequestException
import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.util.{Failure, Success}
import scalaj.http.HttpRequest


class ImageApiClientTest extends UnitSuite with UnitTestEnvironment {

  override val imageApiClient = new ImageApiClient

  val DefaultImage = ImageMetaInformation("1", ImageVariants(Some(Image("thumb", 10, "contentType")), Some(Image("full", 1000, "contentType"))))

  test("That some metaInfo is returned when images is found") {
    when(ndlaClient.fetch[ImageMetaInformation](any[HttpRequest])(any[Manifest[ImageMetaInformation]])).thenReturn(Success(DefaultImage))

    val imageMeta = imageApiClient.getImageMetaInformationForExternId("abc")
    imageMeta.isDefined should be (true)
    imageMeta.get.id should equal("1")
    imageMeta.get.images.full.get.size should be (1000)
    imageMeta.get.images.small.get.size should be (10)
  }

  test("That none is returned when http 404 is received") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(true)
    when(ndlaClient.fetch[ImageMetaInformation](any[HttpRequest])(any[Manifest[ImageMetaInformation]])).thenReturn(Failure(exception))

    imageApiClient.getImageMetaInformationForExternId("abc") should be (None)
  }

  test("That exception is returned when http-error") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(false)
    when(ndlaClient.fetch[ImageMetaInformation](any[HttpRequest])(any[Manifest[ImageMetaInformation]])).thenReturn(Failure(exception))

    intercept[HttpRequestException]{
      imageApiClient.getImageMetaInformationForExternId("abc")
      fail("Exception should have been thrown")
    } should be (exception)

    verify(exception, times(1)).is404
  }

  test("That exception is returned for a randomly chosen exception") {
    val exception = mock[NoSuchElementException]
    when(ndlaClient.fetch[ImageMetaInformation](any[HttpRequest])(any[Manifest[ImageMetaInformation]])).thenReturn(Failure(exception))

    intercept[NoSuchElementException]{
      imageApiClient.getImageMetaInformationForExternId("abc")
      fail("Exception should have been thrown")
    } should be (exception)
  }
}
