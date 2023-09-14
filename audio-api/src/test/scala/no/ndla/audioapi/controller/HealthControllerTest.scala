/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import cats.effect.unsafe.implicits.global
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.{Author, Tag, Title}
import no.ndla.network.tapir.Service
import sttp.client3.Response
import sttp.model.StatusCode
import sttp.client3.quick._

class HealthControllerTest extends UnitSuite with TestEnvironment {
  implicit val formats = org.json4s.DefaultFormats

  val serverPort: Int = findFreePort

  val httpResponseMock: Response[String] = mock[Response[String]]

  lazy val controller = new HealthController {
    override def getApiResponse(url: String): Response[String] = httpResponseMock
  }

  override val services: List[Service[Eff]] = List(controller)
  override def beforeAll(): Unit = {
    Routes.startJdkServer("HealthControllerTest", serverPort) {}.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  controller.setWarmedUp()

  val updated = NDLADate.of(2017, 4, 1, 12, 15, 32)
  val created = NDLADate.of(2017, 3, 1, 12, 15, 32)

  val copyrighted =
    Copyright("copyrighted", Some("New York"), Seq(Author("Forfatter", "Clark Kent")), Seq(), Seq(), None, None)

  val audioMeta = domain.AudioMetaInformation(
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

  when(httpResponseMock.code).thenReturn(StatusCode.NotFound)
  when(audioRepository.getRandomAudio()).thenReturn(None)

  test("that /health returns 200 on success") {
    when(httpResponseMock.code).thenReturn(StatusCode.Ok)
    when(audioRepository.getRandomAudio()).thenReturn(Some(audioMeta))

    val request =
      quickRequest
        .get(uri"http://localhost:$serverPort/health")

    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
  }

  test("that /health returns 500 on failure") {
    when(httpResponseMock.code).thenReturn(StatusCode.InternalServerError)
    when(audioRepository.getRandomAudio()).thenReturn(Some(audioMeta))

    val request =
      quickRequest
        .get(uri"http://localhost:$serverPort/health")

    val response = simpleHttpClient.send(request)
    response.code.code should be(500)
  }

  test("that /health returns 200 on no images") {
    when(httpResponseMock.code).thenReturn(StatusCode.NotFound)
    when(audioRepository.getRandomAudio()).thenReturn(None)

    val request =
      quickRequest
        .get(uri"http://localhost:$serverPort/health")

    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
  }

}
