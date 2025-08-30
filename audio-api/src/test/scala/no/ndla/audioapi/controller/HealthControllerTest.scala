/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.domain.*
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import no.ndla.common.Clock
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain.{Author, ContributorType, Tag, Title}
import no.ndla.mapping.License
import no.ndla.network.tapir.{ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.Mockito.when
import sttp.client3.quick.*

class HealthControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  var healthControllerResponse: Int                                 = 200
  override implicit lazy val clock: Clock                           = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers             = new ErrorHelpers
  override implicit lazy val errorHandling: ControllerErrorHandling = new ControllerErrorHandling
  val controller: HealthController                                  = new HealthController
  override implicit lazy val services: List[TapirController]        = List(controller)
  override implicit lazy val routes: Routes                         = new Routes
  controller.setWarmedUp()

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  val updated: NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)
  val created: NDLADate = NDLADate.of(2017, 3, 1, 12, 15, 32)

  val copyrighted: Copyright =
    Copyright(
      License.Copyrighted.toString,
      Some("New York"),
      Seq(Author(ContributorType.Writer, "Clark Kent")),
      Seq(),
      Seq(),
      None,
      None,
      false
    )

  val audioMeta: AudioMetaInformation = domain.AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(Title("Batmen er på vift med en bil", "nb")),
    Seq(Audio("file.mp3", "audio/mpeg", 1024, "nb")),
    copyrighted,
    Seq(Tag(Seq("fisk"), "nb")),
    "ndla124",
    updated,
    created,
    Seq.empty,
    AudioType.Standard,
    Seq.empty,
    None,
    None
  )

  healthControllerResponse = 404
  when(audioRepository.getRandomAudio()).thenReturn(None)

  test("that /health/readiness returns 200 on success") {
    healthControllerResponse = 200
    when(audioRepository.getRandomAudio()).thenReturn(Some(audioMeta))
    when(s3Client.objectExists("file.mp3")).thenReturn(true)

    val request =
      quickRequest
        .get(uri"http://localhost:$serverPort/health/readiness")

    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
  }

  test("that /health/readiness returns 500 on failure") {
    healthControllerResponse = 500
    when(audioRepository.getRandomAudio()).thenReturn(Some(audioMeta))
    when(s3Client.objectExists("file.mp3")).thenReturn(false)

    val request =
      quickRequest
        .get(uri"http://localhost:$serverPort/health/readiness")

    val response = simpleHttpClient.send(request)
    response.code.code should be(500)
  }

  test("that /health/liveness returns 200") {
    val request =
      quickRequest
        .get(uri"http://localhost:$serverPort/health/liveness")

    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
  }

  test("that /health returns 200 on no audios") {
    healthControllerResponse = 404
    when(audioRepository.getRandomAudio()).thenReturn(None)
    when(s3Client.objectExists("file.mp3")).thenReturn(false)

    val request =
      quickRequest
        .get(uri"http://localhost:$serverPort/health/readiness")

    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
  }

}
