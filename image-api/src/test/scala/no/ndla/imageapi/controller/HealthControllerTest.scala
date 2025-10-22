/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.Clock
import no.ndla.common.model.NDLADate
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.common.model.domain.{Author, ContributorType}
import no.ndla.common.model.domain.article.Copyright
import no.ndla.imageapi.model.domain.{
  ImageAltText,
  ImageCaption,
  ImageDimensions,
  ImageFileData,
  ImageMetaInformation,
  ImageTitle,
  ModelReleasedStatus,
}
import no.ndla.tapirtesting.TapirControllerTest
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.mapping.License
import org.mockito.Mockito.when
import sttp.client3.quick.*

class HealthControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock: Clock                    = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  var healthControllerResponse: Int                          = 200
  val controller: HealthController                           = new HealthController
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes
  controller.setWarmedUp()

  val updated: NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)
  val created: NDLADate = NDLADate.of(2017, 3, 1, 12, 15, 32)

  val copyrighted: Copyright = Copyright(
    License.Copyrighted.toString,
    Some("New York"),
    Seq(Author(ContributorType.Writer, "Clark Kent")),
    Seq(),
    Seq(),
    None,
    None,
    processed = false,
  )

  val imageMeta: ImageMetaInformation = ImageMetaInformation(
    Some(1),
    Seq(ImageTitle("Batmen er på vift med en bil", "nb")),
    Seq(ImageAltText("Batmen er på vift med en bil", "nb")),
    Some(Seq(ImageFileData(1, "file.jpg", 1024, "image/jpg", Some(ImageDimensions(1, 1)), "nb", 1))),
    copyrighted,
    Seq.empty,
    Seq(ImageCaption("Batmen er på vift med en bil", "nb")),
    "ndla124",
    updated,
    created,
    "ndla124",
    ModelReleasedStatus.NOT_APPLICABLE,
    Seq.empty,
  )

  test("that /health/readiness returns 200 on success") {
    healthControllerResponse = 200
    when(imageRepository.getRandomImage()).thenReturn(Some(imageMeta))
    when(imageStorage.objectExists("file.jpg")).thenReturn(true)

    val request = quickRequest.get(uri"http://localhost:$serverPort/health/readiness")

    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
  }

  test("that /health/liveness returns 200") {
    healthControllerResponse = 200
    val request = quickRequest.get(uri"http://localhost:$serverPort/health/liveness")

    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
  }

  test("that /health/readiness returns 500 on failure") {
    healthControllerResponse = 500
    when(imageRepository.getRandomImage()).thenReturn(Some(imageMeta))
    when(imageStorage.objectExists("file.jpg")).thenReturn(false)

    val request = quickRequest.get(uri"http://localhost:$serverPort/health/readiness")

    val response = simpleHttpClient.send(request)
    response.code.code should be(500)
  }
}
