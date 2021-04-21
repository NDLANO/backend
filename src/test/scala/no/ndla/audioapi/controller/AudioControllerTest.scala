/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.api._
import no.ndla.audioapi.model.domain.SearchSettings
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.{AudioSwagger, TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers._
import org.scalatra.servlet.FileItem
import org.scalatra.test.Uploadable
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success}

class AudioControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXVkaW8tdGVzdDp3cml0ZSIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.YYRWLfDDfnyyw6mDoOsvYEJtHf3uoJlkCUMmLKV1lXI"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.6umgx7Xu8cnoBsry1NGL0iBe32wUuqCpLrospDlLmVc"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoic29tZTpvdGhlciIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.Hbmh9KX19nx7yT3rEcP9pyzRO0uQJBRucfqH9QEZtLyXjYj_fAyOhsoicOVEbHSES7rtdiJK43-gijSpWWmGWOkE6Ym7nHGhB_nLdvp_25PDgdKHo-KawZdAyIcJFr5_t3CJ2Z2IPVbrXwUd99vuXEBaV0dMwkT0kDtkwHuS-8E"

  implicit val swagger: AudioSwagger = new AudioSwagger
  lazy val controller = new AudioController
  addServlet(controller, "/*")

  val sampleUploadFile: Uploadable = new Uploadable {
    override def contentLength = 3
    override def content: Array[Byte] = Array[Byte](0x49, 0x44, 0x33)
    override def contentType = "audio/mp3"
    override def fileName = "test.mp3"
  }

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
    post("/", Map("metadata" -> sampleNewAudioMeta)) {
      status should equal(403)
    }
  }

  test("That POST / returns 400 if parameters are missing") {
    post("/", Map("metadata" -> sampleNewAudioMeta), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("That POST / returns 200 if everything is fine and dandy") {
    val sampleAudioMeta =
      AudioMetaInformation(
        1,
        1,
        Title("title", "nb"),
        Audio("", "", -1, "nb"),
        Copyright(License("by", None, None), None, Seq(), Seq(), Seq(), None, None, None),
        Tag(Seq(), "nb"),
        Seq("nb"),
        "standard",
        None,
        None
      )
    when(writeService.storeNewAudio(any[NewAudioMetaInformation], any[FileItem])).thenReturn(Success(sampleAudioMeta))

    post("/",
         Map("metadata" -> sampleNewAudioMeta),
         Map("file" -> sampleUploadFile),
         headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(200)
    }
  }

  test("That POST / returns 500 if an unexpected error occurs") {
    val runtimeMock = mock[RuntimeException]
    doNothing.when(runtimeMock).printStackTrace()
    when(runtimeMock.getMessage).thenReturn("Something (not really) wrong (this is a test hehe)")

    when(writeService.storeNewAudio(any[NewAudioMetaInformation], any[FileItem]))
      .thenReturn(Failure(runtimeMock))

    post("/",
         Map("metadata" -> sampleNewAudioMeta),
         Map("file" -> sampleUploadFile),
         headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(500)
    }
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    post("/", Map("metadata" -> sampleNewAudioMeta), headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal(403)
    }
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    post("/", Map("metadata" -> sampleNewAudioMeta), headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal(403)
    }
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

    get(s"/") {
      status should be(200)
      body.contains(scrollId) should be(false)
      response.getHeader("search-context") should be(scrollId)
    }
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

    get(s"/?search-context=$scrollId") {
      status should be(200)
    }

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

    post(s"/search/", body = s"""{"scrollId":"$scrollId"}""") {
      status should be(200)
    }

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

    post(s"/search/", body = s"""{"scrollId":"initial"}""") {
      status should be(200)
    }

    verify(audioSearchService, times(1)).matchingQuery(expectedSettings)
    verify(audioSearchService, times(0)).scroll(any[String], any[String])
  }
}
