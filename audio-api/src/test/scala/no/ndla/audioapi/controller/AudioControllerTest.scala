/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.api._
import no.ndla.audioapi.model.domain.SearchSettings
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.TapirServer
import org.mockito.ArgumentMatchers._
import org.scalatra.servlet.FileItem
import org.scalatra.test.Uploadable
import sttp.client3.quick._
import sttp.model.Part

import java.io.File
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class AudioControllerTest extends UnitSuite with TestEnvironment {
  val serverPort: Int = findFreePort
  val controller      = new AudioController

  override def beforeAll(): Unit = {
    val app    = Routes.build(List(controller))
    val server = TapirServer("AudioControllerTest", serverPort, app, enableMelody = false)()
    server.toFuture
    blockUntil(() => server.isReady)
  }

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXVkaW86d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.BRAWsdX1Djs8GXGq7jj77DLUxyx2BAI86C74xwUEt4E"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.6umgx7Xu8cnoBsry1NGL0iBe32wUuqCpLrospDlLmVc"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoic29tZTpvdGhlciIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.Hbmh9KX19nx7yT3rEcP9pyzRO0uQJBRucfqH9QEZtLyXjYj_fAyOhsoicOVEbHSES7rtdiJK43-gijSpWWmGWOkE6Ym7nHGhB_nLdvp_25PDgdKHo-KawZdAyIcJFr5_t3CJ2Z2IPVbrXwUd99vuXEBaV0dMwkT0kDtkwHuS-8E"

  val sampleUploadFile: Uploadable = new Uploadable {
    override def contentLength        = 3
    override def content: Array[Byte] = Array[Byte](0x49, 0x44, 0x33)
    override def contentType          = "audio/mp3"
    override def fileName             = "test.mp3"
  }

  val mockFile = mock[File]

  val sampleNewAudioMeta: String =
    """
      |{
      |    "title": "Test",
      |    "language": "nb",
      |    "audioFile": "test.mp3",
      |    "copyright": {
      |        "license": {
      |            "license": "by-sa"
      |        },
      |        "authors": []
      |    },
      |    "tags": ["test"]
      |}
    """.stripMargin

  test("That POST / returns 403 if no auth-header") {
    val request =
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .contentType("multipart/form-data")
        .readTimeout(Duration.Inf)
        .multipartBody(multipart("metadata", sampleNewAudioMeta))

    val response = simpleHttpClient.send(request)
    response.code.code should be(403)
  }

  test("That POST / returns 400 if parameters are missing") {
    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .body(Map("metadata" -> sampleNewAudioMeta))
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    response.code.code should be(400)
  }

  test("That POST / returns 200 if everything is fine and dandy") {
    val sampleAudioMeta =
      api.AudioMetaInformation(
        1,
        1,
        Title("title", "nb"),
        Audio("", "", -1, "nb"),
        Copyright(License("by", None, None), None, Seq(), Seq(), Seq(), None, None, None),
        Tag(Seq(), "nb"),
        Seq("nb"),
        "podcast",
        None,
        None,
        None,
        TestData.yesterday,
        TestData.today
      )
    when(writeService.storeNewAudio(any[NewAudioMetaInformation], any[Part[File]], any))
      .thenReturn(Success(sampleAudioMeta))

    val file     = multipartFile("file", mockFile)
    val metadata = multipart("metadata", sampleNewAudioMeta)

    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .multipartBody(file, metadata)
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    response.code.code should be(200)
    response.body.contains("audioType\":\"podcast\"") should be(true)
  }

  test("That POST / returns 500 if an unexpected error occurs") {
    val runtimeMock = mock[RuntimeException](withSettings.lenient())
    doNothing.when(runtimeMock).printStackTrace()
    when(runtimeMock.getMessage).thenReturn("Something (not really) wrong (this is a test hehe)")

    when(writeService.storeNewAudio(any[NewAudioMetaInformation], any, any))
      .thenReturn(Failure(runtimeMock))

    val file     = multipartFile("file", mockFile)
    val metadata = multipart("metadata", sampleNewAudioMeta)

    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .multipartBody(file, metadata)
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )

    response.code.code should be(500)

  }

  test("That POST / returns 403 if auth header does not have expected role") {
    val metadata = multipart("metadata", sampleNewAudioMeta)
    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .multipartBody(metadata)
        .headers(Map("Authorization" -> authHeaderWithWrongRole))
    )
    response.code.code should be(403)
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    val metadata = multipart("metadata", sampleNewAudioMeta)
    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .multipartBody(metadata)
        .headers(Map("Authorization" -> authHeaderWithoutAnyRoles))
    )
  }

  test("That scrollId is in header, and not in body") {
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[api.AudioSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )
    when(audioSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchResponse))

    val response = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/audio-api/v1/audio")
    )
    response.code.code should be(200)
    response.body.contains(scrollId) should be(false)
    response.header("search-context") should be(scrollId)
  }

  test("That scrolling uses scroll and not searches normally") {
    reset(audioSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[api.AudioSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )

    when(audioSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))

    val response = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/audio-api/v1/audio?search-context=$scrollId")
    )
    response.code.code should be(200)

    verify(audioSearchService, times(0)).matchingQuery(any[SearchSettings])
    verify(audioSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("That scrolling with POST uses scroll and not searches normally") {
    reset(audioSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[api.AudioSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )

    when(audioSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))

    val response = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/audio-api/v1/audio/search/")
        .body(s"""{"scrollId":"$scrollId"}""")
        .contentType("application/json")
    )
    response.code.code should be(200)

    verify(audioSearchService, times(0)).matchingQuery(any[SearchSettings])
    verify(audioSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("That initial scroll-context searches normally") {
    reset(audioSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[api.AudioSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )

    when(audioSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchResponse))

    val expectedSettings = TestData.searchSettings.copy(
      shouldScroll = true
    )

    val response = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/audio-api/v1/audio/search/")
        .body(s"""{"scrollId":"initial"}""")
        .contentType("application/json")
    )
    response.code.code should be(200)

    verify(audioSearchService, times(1)).matchingQuery(expectedSettings)
    verify(audioSearchService, times(0)).scroll(any[String], any[String])
  }
}
