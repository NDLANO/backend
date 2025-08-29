/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.{CirceUtil, Clock}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Tag
import no.ndla.common.model.domain.article.Copyright
import no.ndla.imageapi.model.api.{
  ImageMetaSummaryDTO,
  NewImageMetaInformationV2DTO,
  SearchResultDTO,
  UpdateImageMetaInformationDTO
}
import no.ndla.imageapi.model.domain.*
import no.ndla.imageapi.model.{ImageNotFoundException, api, domain}
import no.ndla.imageapi.service.ConverterService
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.mapping.License
import no.ndla.mapping.License.CC_BY
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.{reset, times, verify, when, withSettings}
import org.mockito.quality.Strictness
import sttp.client3.quick.*

import scala.util.{Failure, Success, Try}

class ImageControllerV2Test extends UnitSuite with TestEnvironment with TapirControllerTest {
  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJpbWFnZXM6d3JpdGUiXSwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.1_j9R9KML2LTqeAE4bpRByJcR6m6Tv3pTOozpYCnTC8"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6W10sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.vw9YhRtgUQr_vuDhLNHfBsZz-4XLhCc1Kwxi0w0_qGI"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJzb21lOm90aGVyIl0sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.u8o7-FXyVzWurle2tP1pngad8KRja6VjFdmy71T4m0k"

  override implicit lazy val clock: Clock                       = mock[Clock]
  override implicit lazy val converterService: ConverterService = new ConverterService
  override implicit lazy val errorHelpers: ErrorHelpers         = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling       = new ControllerErrorHandling
  override val controller: ImageControllerV2                    = new ImageControllerV2 {
    override val maxImageFileSizeBytes: Int = 10
  }
  override implicit lazy val services: List[TapirController] = List(controller)
  override lazy val routes                                   = new Routes

  override def beforeEach(): Unit = {
    reset(clock)
    reset(searchConverterService)
    when(clock.now()).thenCallRealMethod()
  }

  val fileBody: Array[Byte]        = Array[Byte](-1, -40, -1)
  val sampleNewImageMetaV2: String =
    """
      |{
      |  "title":"test1",
      |  "alttext":"test2",
      |  "copyright": {
      |    "license": {
      |      "license": "CC-BY-SA-4.0",
      |      "description": "Creative Commons Attribution-ShareAlike 4.0 Generic",
      |      "url": "https:\/\/creativecommons.org\/licenses\/by-sa\/4.0\/"
      |    },
      |    "origin": "",
      |    "processed": false,
      |    "creators": [],
      |    "rightsholders": [],
      |    "processors": [
      |      {
      |        "type": "writer",
      |        "name": "Wenche Heir"
      |      }
      |    ]
      |  },
      |  "tags": [
      |    "lel"
      |  ],
      |  "caption": "captionheredude",
      |  "language": "no"
      |}
    """.stripMargin

  val sampleUpdateImageMeta: String =
    """
      |{
      | "title":"TestTittel",
      | "alttext":"TestAltText",
      | "language":"nb"
      |}
    """.stripMargin

  test("That GET / returns body and 200") {
    val expectedBody       = """{"totalCount":0,"page":1,"pageSize":10,"language":"nb","results":[]}"""
    val domainSearchResult = domain.SearchResult[ImageMetaSummaryDTO](0, Some(1), 10, "nb", List(), None)
    val apiSearchResult    = SearchResultDTO(0, Some(1), 10, "nb", List())
    when(imageSearchService.matchingQuery(any[SearchSettings], any)).thenReturn(Success(domainSearchResult))
    when(searchConverterService.asApiSearchResult(domainSearchResult)).thenReturn(apiSearchResult)
    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/image-api/v2/images")
    )
    res.code.code should be(200)
    res.body should be(expectedBody)
  }

  test("That GET / returns body and 200 when image exists") {

    val date = NDLADate
      .now()
      .withYear(2021)
      .withMonth(4)
      .withDayOfMonth(1)
      .withHour(12)
      .withMinute(34)
      .withSecond(56)
      .withNano(0)

    val imageSummary = api.ImageMetaSummaryDTO(
      "4",
      api.ImageTitleDTO("Tittel", "nb"),
      Seq("Jason Bourne", "Ben Affleck"),
      api.ImageAltTextDTO("AltText", "nb"),
      api.ImageCaptionDTO("Caption", "nb"),
      "http://image-api.ndla-local/image-api/raw/4",
      "http://image-api.ndla-local/image-api/v2/images/4",
      License.CC_BY_SA.toString,
      Seq("nb"),
      Some("yes"),
      None,
      date,
      123,
      "image/jpg",
      None
    )
    val expectedBody =
      s"""{"totalCount":1,"page":1,"pageSize":10,"language":"nb","results":[{"id":"4","title":{"title":"Tittel","language":"nb"},"contributors":["Jason Bourne","Ben Affleck"],"altText":{"alttext":"AltText","language":"nb"},"caption":{"caption":"Caption","language":"nb"},"previewUrl":"http://image-api.ndla-local/image-api/raw/4","metaUrl":"http://image-api.ndla-local/image-api/v2/images/4","license":"CC-BY-SA-4.0","supportedLanguages":["nb"],"modelRelease":"yes","lastUpdated":"${date.asString}","fileSize":123,"contentType":"image/jpg"}]}"""
    val domainSearchResult = domain.SearchResult(1, Some(1), 10, "nb", List(imageSummary), None)
    val apiSearchResult    = api.SearchResultDTO(1, Some(1), 10, "nb", List(imageSummary))
    when(imageSearchService.matchingQuery(any[SearchSettings], any)).thenReturn(Success(domainSearchResult))
    when(searchConverterService.asApiSearchResult(domainSearchResult)).thenReturn(apiSearchResult)
    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/image-api/v2/images")
    )
    res.code.code should be(200)
    res.body should be(expectedBody)
  }

  test("That GET /<id> returns 404 when image does not exist") {
    when(readService.withId(123, None, None)).thenReturn(Success(None))
    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/image-api/v2/images/123")
    )
    res.code.code should be(404)
  }

  test("That GET /<id> returns body and 200 when image exists") {
    val testUrl      = "http://test.test/1"
    val expectedBody =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"CC-BY-NC-SA-4.0","description":"Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International","url":"https://creativecommons.org/licenses/by-nc-sa/4.0/"},"origin":"http://www.scanpix.no","creators":[{"type":"photographer","name":"Test Testesen"}],"processors":[{"type":"editorial","name":"Kåre Knegg"}],"rightsholders":[{"type":"supplier","name":"Leverans Leveransensen"}],"processed":false},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""
    val expectedObject = CirceUtil.unsafeParseAs[api.ImageMetaInformationV2DTO](expectedBody)
    when(readService.withId(1, None, None)).thenReturn(Success(Option(expectedObject)))

    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/image-api/v2/images/1")
    )
    res.code.code should be(200)
    val result = CirceUtil.unsafeParseAs[api.ImageMetaInformationV2DTO](res.body)
    result.copy(imageUrl = testUrl, metaUrl = testUrl) should equal(expectedObject)
  }

  test("That GET /<id> returns body with license and authors") {
    val testUrl      = "http://test.test/1"
    val expectedBody =
      s"""
         |{
         |  "id":"1",
         |  "metaUrl":"$testUrl",
         |  "title":{"title":"Elg i busk","language":"nb"},
         |  "created":"2017-04-01T12:15:32Z",
         |  "createdBy":"ndla124",
         |  "modelRelease":"yes",
         |  "alttext":{"alttext":"Elg i busk","language":"nb"},
         |  "imageUrl":"$testUrl",
         |  "size":2865539,
         |  "contentType":"image/jpeg",
         |  "copyright":{
         |    "license":{
         |      "license":"CC-BY-NC-SA-4.0",
         |      "description":"Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International",
         |      "url":"https://creativecommons.org/licenses/by-nc-sa/4.0/"
         |    },
         |    "origin":"http://www.scanpix.no",
         |    "creators":[{"type":"writer","name":"Knutulf Knagsen"}],
         |    "processors":[{"type":"editorial","name":"Kåre Knegg"}],
         |    "rightsholders":[],
         |    "processed":false
         |  },
         |  "tags":{"tags":["rovdyr","elg"],"language":"nb"},
         |  "caption":{"caption":"Elg i busk","language":"nb"},
         |  "supportedLanguages":["nb"]
         |}""".stripMargin
    val expectedObject = CirceUtil.unsafeParseAs[api.ImageMetaInformationV2DTO](expectedBody)

    when(readService.withId(1, None, None)).thenReturn(Success(Option(expectedObject)))

    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/image-api/v2/images/1")
    )
    res.code.code should be(200)
    val result = CirceUtil.unsafeParseAs[api.ImageMetaInformationV2DTO](res.body)
    result.copy(imageUrl = testUrl, metaUrl = testUrl) should equal(expectedObject)
  }

  test("That POST / returns 401 if no auth-header") {

    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/image-api/v2/images")
        .multipartBody(multipart("metadata", sampleNewImageMetaV2))
    )
    res.code.code should be(401)
  }

  test("That POST / returns 400 if parameters are missing") {
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/image-api/v2/images")
        .multipartBody(multipart("metadata", sampleNewImageMetaV2))
        .header("Authorization", authHeaderWithWriteRole)
    )
    res.code.code should equal(400)
  }

  test("That POST / returns 200 if everything went well") {
    val titles: Seq[ImageTitle]     = Seq()
    val alttexts: Seq[ImageAltText] = Seq()
    val copyright      = Copyright(CC_BY.toString, None, Seq.empty, Seq.empty, Seq.empty, None, None, false)
    val tags: Seq[Tag] = Seq()
    val captions: Seq[ImageCaption] = Seq()

    val sampleImageMeta = new ImageMetaInformation(
      id = Some(1),
      titles = titles,
      alttexts = alttexts,
      images = Some(
        Seq(
          new ImageFileData(
            id = 1,
            fileName = "/img.jpg",
            size = 1024,
            contentType = "image/jpeg",
            dimensions = None,
            language = "und",
            imageMetaId = 1
          )
        )
      ),
      copyright = copyright,
      tags = tags,
      captions = captions,
      updatedBy = "updatedBy",
      updated = NDLADate.now(),
      created = NDLADate.now(),
      createdBy = "createdBy",
      modelReleased = ModelReleasedStatus.YES,
      editorNotes = Seq.empty
    )

    when(writeService.storeNewImage(any[NewImageMetaInformationV2DTO], any, any))
      .thenReturn(Success(sampleImageMeta))

    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/image-api/v2/images")
        .multipartBody[Any](
          multipart("metadata", sampleNewImageMetaV2),
          multipart("file", fileBody)
        )
        .header("Authorization", authHeaderWithWriteRole)
    )
    res.code.code should equal(200)
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/image-api/v2/images")
        .multipartBody[Any](
          multipart("metadata", sampleNewImageMetaV2),
          multipart("file", fileBody)
        )
        .header("Authorization", authHeaderWithWrongRole)
    )
    res.code.code should equal(403)
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/image-api/v2/images")
        .multipartBody[Any](
          multipart("metadata", sampleNewImageMetaV2),
          multipart("file", fileBody)
        )
        .header("Authorization", authHeaderWithoutAnyRoles)
    )
    res.code.code should equal(403)
  }

  test("That POST / returns 413 if file is too big") {
    val tooBigFile =
      multipart("file", Array[Byte](0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20, 0x21))
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/image-api/v2/images")
        .multipartBody[Any](
          multipart("metadata", sampleNewImageMetaV2),
          tooBigFile
        )
        .header("Authorization", authHeaderWithWriteRole)
    )
    res.code.code should equal(413)
  }

  test("That POST / returns 500 if an unexpected error occurs") {
    reset(writeService)
    val exceptionMock = mock[RuntimeException](withSettings.strictness(Strictness.LENIENT))
    when(writeService.storeNewImage(any[NewImageMetaInformationV2DTO], any, any))
      .thenReturn(Failure(exceptionMock))

    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/image-api/v2/images")
        .multipartBody[Any](
          multipart("metadata", sampleNewImageMetaV2),
          multipart("file", fileBody)
        )
        .header("Authorization", authHeaderWithWriteRole)
    )
    res.code.code should be(500)
  }

  test("That PATCH /<id> returns 200 when everything went well") {
    reset(writeService)
    when(writeService.updateImage(any[Long], any[UpdateImageMetaInformationDTO], any, any))
      .thenReturn(Try(TestData.apiElg))
    val res = simpleHttpClient.send(
      quickRequest
        .patch(uri"http://localhost:$serverPort/image-api/v2/images/1")
        .multipartBody[Any](
          multipart("metadata", sampleUpdateImageMeta)
        )
        .header("Authorization", authHeaderWithWriteRole)
    )
    res.code.code should be(200)
  }

  test("That PATCH /<id> returns 404 when image doesn't exist") {
    reset(writeService)
    when(writeService.updateImage(any[Long], any[UpdateImageMetaInformationDTO], any, any))
      .thenThrow(new ImageNotFoundException(s"Image with id 1 not found"))
    val res = simpleHttpClient.send(
      quickRequest
        .patch(uri"http://localhost:$serverPort/image-api/v2/images/1")
        .multipartBody[Any](
          multipart("metadata", sampleUpdateImageMeta)
        )
        .header("Authorization", authHeaderWithWriteRole)
    )
    res.code.code should be(404)
  }

  test("That PATCH /<id> returns 403 when not permitted") {
    val res = simpleHttpClient.send(
      quickRequest
        .patch(uri"http://localhost:$serverPort/image-api/v2/images/1")
        .multipartBody[Any](
          multipart("metadata", sampleUpdateImageMeta)
        )
        .header("Authorization", authHeaderWithoutAnyRoles)
    )
    res.code.code should be(403)
  }

  test("That scrollId is in header, and not in body") {
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[ImageMetaSummaryDTO](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )
    when(imageSearchService.matchingQuery(any[SearchSettings], any)).thenReturn(Success(searchResponse))
    when(searchConverterService.asApiSearchResult(any)).thenCallRealMethod()

    val res = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/image-api/v2/images/"))
    res.code.code should be(200)
    res.body.contains(scrollId) should be(false)
    res.header("search-context") should be(Some(scrollId))
  }

  test("That scrolling uses scroll and not searches normally") {
    reset(imageSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[ImageMetaSummaryDTO](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )

    when(imageSearchService.scrollV2(anyString, anyString, any)).thenReturn(Success(searchResponse))
    when(searchConverterService.asApiSearchResult(any)).thenCallRealMethod()

    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/image-api/v2/images/?search-context=$scrollId")
    )
    res.code.code should be(200)
    verify(imageSearchService, times(0)).matchingQuery(any[SearchSettings], any)
    verify(imageSearchService, times(1)).scrollV2(eqTo(scrollId), any[String], any)
  }

  test("That scrolling with POST uses scroll and not searches normally") {
    reset(imageSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[ImageMetaSummaryDTO](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )

    when(imageSearchService.scrollV2(anyString, anyString, any)).thenReturn(Success(searchResponse))
    when(searchConverterService.asApiSearchResult(any)).thenCallRealMethod()

    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/image-api/v2/images/search/")
        .body(s"""{"scrollId":"$scrollId"}""")
    )
    res.code.code should be(200)

    verify(imageSearchService, times(0)).matchingQuery(any[SearchSettings], any)
    verify(imageSearchService, times(1)).scrollV2(eqTo(scrollId), any[String], any)
  }

  test("that initial search-context doesn't scroll") {
    reset(imageSearchService)

    val expectedSettings = TestData.searchSettings.copy(
      shouldScroll = true,
      sort = Sort.ByTitleAsc
    )

    val result = domain.SearchResult[api.ImageMetaSummaryDTO](
      totalCount = 0,
      page = None,
      pageSize = 10,
      language = "*",
      results = Seq.empty,
      scrollId = Some("heiheihei")
    )
    when(searchConverterService.asApiSearchResult(any)).thenCallRealMethod()
    when(imageSearchService.matchingQuery(any[SearchSettings], any)).thenReturn(Success(result))
    val res = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/image-api/v2/images/?search-context=initial")
    )

    res.code.code should be(200)
    verify(imageSearchService, times(1)).matchingQuery(eqTo(expectedSettings), any)
    verify(imageSearchService, times(0)).scrollV2(any[String], any[String], any)

  }
}
