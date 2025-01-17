/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import io.circe.parser
import no.ndla.audioapi.model.api.*
import no.ndla.audioapi.model.domain.SearchSettings
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.common.CirceUtil.unsafeParseAs
import no.ndla.common.model.api.{CopyrightDTO, LicenseDTO}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{doNothing, reset, times, verify, when, withSettings}
import org.mockito.quality.Strictness
import org.scalatest.tagobjects.Retryable
import org.scalatest.{Canceled, Failed, Outcome, Retries}
import sttp.client3.quick.*

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class AudioControllerTest extends UnitSuite with TestEnvironment with Retries with TapirControllerTest {
  val controller: AudioController = new AudioController {
    // NOTE: Small max file size when testing to test the failure in the controller without using a bunch of memory
    override val maxAudioFileSizeBytes: Int = 10
  }

  val maxRetries = 5
  def withFixture(test: NoArgTest, count: Int): Outcome = {
    val outcome = Try(super.withFixture(test))
    outcome match {
      case Success(Failed(_)) | Success(Canceled(_)) | Failure(_) =>
        println(s"'${test.name}' failed, retrying $count more times...")
        if (count == 1) super.withFixture(test) else withFixture(test, count - 1)
      case Success(other) =>
        val attemptNum = maxRetries - (count - 1)
        println(s"Retryable test '${test.name}' succeeded on attempt $attemptNum")
        other
    }
  }
  override def withFixture(test: NoArgTest): Outcome = if (isRetryable(test)) {
    withFixture(test, maxRetries)
  } else {
    super.withFixture(test)
  }

  when(clock.now()).thenCallRealMethod()

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiYXpwIjoiMTIzIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXVkaW86d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMiLCJwZXJtaXNzaW9ucyI6WyJhdWRpbzp3cml0ZSJdfQ.jOyT-eIsra1liu_ahLynDCBZe6ltlimsY8hh6Y2dk7g"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJpc3MiOiJodHRwczovL25kbGEtdGVzdC5ldS5hdXRoMC5jb20vIiwic3ViIjoiZnNleE9DZkpGR09LdXkxQzJlNzFPc3ZRd3EwTldLQUtAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNjgzODg0OTQ4LCJleHAiOjE2ODM4OTkzNDgsImF6cCI6ImZzZXhPQ2ZKRkdPS3V5MUMyZTcxT3N2UXdxME5XS0FLIiwic2NvcGUiOiIiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMiLCJwZXJtaXNzaW9ucyI6W119.oznL95qVdP7HFTwwVm8ZfVv1GSW3_mNKTbAt9No8-PI"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJhenAiOiIxMjMiLCJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoic29tZTpvdGhlciIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.1MYhQy2jRMsmyhaUURBC1rFXjCLSJMQyhuWBlB8qLQE"

  val fileBody: Array[Byte] = Array[Byte](0x49, 0x44, 0x33)

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
      |        "origin": "",
      |        "creators": [],
      |        "processors": [],
      |        "rightsholders": [],
      |        "processed": false
      |    },
      |    "tags": ["test"]
      |}
    """.stripMargin

  test("That POST / returns 401 if no auth-header") {
    val request =
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .contentType("multipart/form-data")
        .readTimeout(Duration.Inf)
        .multipartBody(multipart("metadata", sampleNewAudioMeta))

    val response = simpleHttpClient.send(request)
    response.code.code should be(401)
  }

  test("That POST / returns 400 if parameters are missing", Retryable) {
    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .readTimeout(Duration.Inf)
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
        .multipartBody(multipart("metadata", sampleNewAudioMeta))
    )
    response.code.code should be(400)
  }

  test("That POST / returns 200 if everything is fine and dandy", Retryable) {
    val sampleAudioMeta =
      api.AudioMetaInformationDTO(
        1,
        1,
        TitleDTO("title", "nb"),
        AudioDTO("", "", -1, "nb"),
        CopyrightDTO(LicenseDTO("by", None, None), None, Seq(), Seq(), Seq(), None, None, false),
        TagDTO(Seq(), "nb"),
        Seq("nb"),
        "podcast",
        None,
        None,
        None,
        TestData.yesterday,
        TestData.today
      )
    when(writeService.storeNewAudio(any[NewAudioMetaInformationDTO], any, any)).thenReturn(Success(sampleAudioMeta))

    val file     = multipart("file", fileBody)
    val metadata = multipart("metadata", sampleNewAudioMeta)

    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .multipartBody[Any](metadata, file)
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    response.code.code should be(200)
    response.body.contains("audioType\":\"podcast\"") should be(true)
  }

  test("That POST / returns 500 if an unexpected error occurs") {
    val runtimeMock = mock[RuntimeException](withSettings.strictness(Strictness.LENIENT))
    doNothing.when(runtimeMock).printStackTrace()
    when(runtimeMock.getMessage).thenReturn("Something (not really) wrong (this is a test hehe)")

    when(writeService.storeNewAudio(any[NewAudioMetaInformationDTO], any, any))
      .thenReturn(Failure(runtimeMock))

    val file     = multipart("file", fileBody)
    val metadata = multipart("metadata", sampleNewAudioMeta)

    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .multipartBody[Any](file, metadata)
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )

    response.code.code should be(500)

  }

  test("That POST / returns 403 if auth header does not have expected role") {
    val metadata = multipart("metadata", sampleNewAudioMeta)
    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .multipartBody[Any](metadata)
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

    response.code.code should be(403)
  }

  test("That scrollId is in header, and not in body") {
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[api.AudioSummaryDTO](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )
    when(audioSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchResponse))
    when(searchConverterService.asApiAudioSummarySearchResult(any)).thenCallRealMethod()

    val response = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/audio-api/v1/audio")
    )
    response.code.code should be(200)
    response.body.contains(scrollId) should be(false)
    response.header("search-context").get should be(scrollId)
  }

  test("That scrolling uses scroll and not searches normally") {
    reset(audioSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[api.AudioSummaryDTO](
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
    val searchResponse = domain.SearchResult[api.AudioSummaryDTO](
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
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio/search/")
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
    val searchResponse = domain.SearchResult[api.AudioSummaryDTO](
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
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio/search/")
        .body(s"""{"scrollId":"initial"}""")
        .contentType("application/json")
    )
    response.code.code should be(200)

    verify(audioSearchService, times(1)).matchingQuery(expectedSettings)
    verify(audioSearchService, times(0)).scroll(any[String], any[String])
  }

  test("That deleting language returns audio if exists") {
    import io.circe.generic.auto._

    when(writeService.deleteAudioLanguageVersion(1, "nb"))
      .thenReturn(Success(Some(TestData.DefaultApiImageMetaInformation)))

    val request = quickRequest
      .delete(uri"http://localhost:$serverPort/audio-api/v1/audio/1/language/nb")
      .headers(Map("Authorization" -> authHeaderWithWriteRole))

    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
    val parsedBody    = parser.parse(response.body)
    val jsonObject    = parsedBody.toTry.get
    val deserializedE = jsonObject.as[api.AudioMetaInformationDTO]
    val deserialized  = deserializedE.toTry.get
    deserialized should be(TestData.DefaultApiImageMetaInformation)
  }

  test("That deleting language returns 204 if last") {
    when(writeService.deleteAudioLanguageVersion(1, "nb"))
      .thenReturn(Success(None))

    val request = quickRequest
      .delete(uri"http://localhost:$serverPort/audio-api/v1/audio/1/language/nb")
      .headers(Map("Authorization" -> authHeaderWithWriteRole))

    val response = simpleHttpClient.send(request)
    response.code.code should be(204)
    response.body should be("")
  }

  test("That GET /ids returns 200 and handles comma separated list") {
    val one = api.AudioMetaInformationDTO(
      1,
      1,
      TitleDTO("one", "nb"),
      AudioDTO("", "", -1, "nb"),
      CopyrightDTO(LicenseDTO("by", None, None), None, Seq(), Seq(), Seq(), None, None, false),
      TagDTO(Seq(), "nb"),
      Seq("nb"),
      "podcast",
      None,
      None,
      None,
      TestData.yesterday,
      TestData.today
    )
    val two   = one.copy(id = 2, title = TitleDTO("two", "nb"))
    val three = one.copy(id = 3, title = TitleDTO("three", "nb"))

    val expectedResult = List(one, two, three)

    when(readService.getAudiosByIds(any, any)).thenReturn(Success(expectedResult))

    val response = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/audio-api/v1/audio/ids/?ids=1,2,3")
    )
    response.code.code should be(200)
    import io.circe.generic.auto._
    val parsedBody = unsafeParseAs[List[api.AudioMetaInformationDTO]](response.body)
    parsedBody should be(expectedResult)

    verify(readService, times(1)).getAudiosByIds(eqTo(List(1, 2, 3)), any)
  }

  test("That GET /?query= doesnt pass empty-string search parameter") {
    reset(audioSearchService, searchConverterService)
    val searchResponse = domain.SearchResult[api.AudioSummaryDTO](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      None
    )

    when(audioSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchResponse))
    when(searchConverterService.asApiAudioSummarySearchResult(any)).thenCallRealMethod()

    val request  = quickRequest.get(uri"http://localhost:$serverPort/audio-api/v1/audio/?query=")
    val response = simpleHttpClient.send(request)
    response.code.code should be(200)

    val argumentCaptor: ArgumentCaptor[SearchSettings] = ArgumentCaptor.forClass(classOf[SearchSettings])
    verify(audioSearchService, times(1)).matchingQuery(argumentCaptor.capture())
    argumentCaptor.getValue.query should be(None)
  }

  test("That uploading a file bigger than max filesize returns 413", Retryable) {
    val sampleAudioMeta =
      api.AudioMetaInformationDTO(
        1,
        1,
        TitleDTO("title", "nb"),
        AudioDTO("", "", -1, "nb"),
        CopyrightDTO(LicenseDTO("by", None, None), None, Seq(), Seq(), Seq(), None, None, false),
        TagDTO(Seq(), "nb"),
        Seq("nb"),
        "podcast",
        None,
        None,
        None,
        TestData.yesterday,
        TestData.today
      )
    when(writeService.storeNewAudio(any[NewAudioMetaInformationDTO], any, any)).thenReturn(Success(sampleAudioMeta))

    val tooBigFile =
      multipart("file", Array[Byte](0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20, 0x21))
    val metadata = multipart("metadata", sampleNewAudioMeta)

    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/audio-api/v1/audio")
        .multipartBody[Any](metadata, tooBigFile)
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    response.code.code should be(413)
  }
}
