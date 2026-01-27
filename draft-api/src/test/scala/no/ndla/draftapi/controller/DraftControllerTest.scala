/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.controller

import no.ndla.common.{CirceUtil, Clock}
import no.ndla.common.errors.AccessDeniedException
import no.ndla.common.model.api.{Delete, Missing, UpdateWith}
import no.ndla.common.model.domain.draft.DraftStatus.EXTERNAL_REVIEW
import no.ndla.common.model.{api as commonApi, domain as common}
import no.ndla.draftapi.TestData.authHeaderWithWriteRole
import no.ndla.draftapi.model.api.ArticleSearchResultDTO
import no.ndla.draftapi.model.domain.{SearchSettings, Sort}
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.mapping.License.getLicenses
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.{reset, times, verify, when}
import sttp.client3.quick.*

import scala.util.{Failure, Success}
import no.ndla.language.Language.findByLanguageOrBestEffort

class DraftControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override val controller: DraftController                   = new DraftController
  override implicit lazy val clock: Clock                    = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes

  override def beforeEach(): Unit = {
    reset(clock)
    reset(searchConverterService)
    when(clock.now()).thenCallRealMethod()
  }

  val updateTitleJson   = """{"revision": 1, "title": "hehe", "language": "nb", "content": "content"}"""
  val invalidArticle    = """{"revision": 1, "title": [{"language": "nb", "titlee": "lol"]}"""    // typo in "titlee"
  val invalidNewArticle = """{ "language": "nb", "content": "<section><h2>Hi</h2></section>" }""" // missing title
  val lang              = "nb"
  val articleId         = 1L

  test("/<article_id> should return 200 if the cover was found withId") {
    when(readService.withId(articleId, lang)).thenReturn(Success(TestData.sampleArticleV2))

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/$articleId?language=$lang")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(200)
  }

  test("/<article_id> should return 404 if the article was not found withId") {
    when(readService.withId(articleId, lang)).thenReturn(Failure(api.NotFoundException("not found yo")))

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/$articleId?language=$lang")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(404)
  }

  test("/<article_id> should return 400 if the article was not found withId") {
    val resp = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/draft-api/v1/drafts/one"))
    resp.code.code should be(400)
  }

  test("That GET /licenses/ with filter sat to by only returns creative common licenses") {
    val creativeCommonlicenses = getLicenses
      .filter(_.license.toString.startsWith("by"))
      .map(l =>
        commonApi.LicenseDTO(
          l.license.toString,
          Option(l.description),
          findByLanguageOrBestEffort(l.url, "en").map(_.url),
        )
      )
      .toSet

    val resp =
      simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/draft-api/v1/drafts/licenses?filter=by"))
    resp.code.code should be(200)
    val convertedBody = CirceUtil.unsafeParseAs[Set[commonApi.LicenseDTO]](resp.body)
    convertedBody should equal(creativeCommonlicenses)
  }

  test("That GET /licenses/ with filter not specified returns all licenses") {
    val allLicenses = getLicenses
      .map(l =>
        commonApi.LicenseDTO(
          l.license.toString,
          Option(l.description),
          findByLanguageOrBestEffort(l.url, "en").map(_.url),
        )
      )
      .toSet
    val resp = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/draft-api/v1/drafts/licenses"))
    resp.code.code should be(200)
    val convertedBody = CirceUtil.unsafeParseAs[Set[commonApi.LicenseDTO]](resp.body)
    convertedBody should equal(allLicenses)
  }

  test("GET / should use size of id-list as page-size if defined") {
    val searchMock = mock[domain.SearchResult[api.ArticleSummaryDTO]]
    when(searchMock.scrollId).thenReturn(None)
    when(articleSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchMock))
    when(searchConverterService.asApiSearchResult(any)).thenReturn(
      ArticleSearchResultDTO(totalCount = 0, page = Some(1), pageSize = 100, results = List.empty)
    )

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/?ids=1,2,3,4&page-size=10&language=nb")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(200)
    verify(articleSearchService, times(1)).matchingQuery(
      TestData
        .searchSettings
        .copy(
          withIdIn = List(1, 2, 3, 4),
          searchLanguage = props.DefaultLanguage,
          page = 1,
          pageSize = 4,
          sort = Sort.ByTitleAsc,
          articleTypes = common.ArticleType.all,
        )
    )
  }

  test("POST / should return 400 if body does not contain all required fields") {
    val resp = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/draft-api/v1/drafts/")
        .body(invalidArticle)
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(400)
  }

  test("POST / should return 201 on created") {
    when(writeService.newArticle(any[api.NewArticleDTO], any[TokenUser])).thenReturn(Success(TestData.sampleArticleV2))
    val bodyStr = CirceUtil.toJsonString(TestData.newArticle)
    val resp    = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/draft-api/v1/drafts/")
        .body(bodyStr)
        .headers(Map("Authorization" -> authHeaderWithWriteRole, "Content-Type" -> "application/json"))
    )
    resp.code.code should be(201)
  }

  test("That / returns a validation message if article is invalid") {
    val resp = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/draft-api/v1/drafts/")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(400)
  }

  test("That POST / returns 401 if no auth-header") {
    val resp = simpleHttpClient.send(quickRequest.post(uri"http://localhost:$serverPort/draft-api/v1/drafts/"))
    resp.code.code should be(401)
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    val resp = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/draft-api/v1/drafts/")
        .headers(Map("Authorization" -> TestData.authHeaderWithoutAnyRoles))
    )
    resp.code.code should be(403)
  }

  test("That GET /<article_id> returns 403 if auth header does not have any roles") {
    when(readService.withId(articleId, lang)).thenReturn(Success(TestData.sampleArticleV2))

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/$articleId?language=$lang")
        .headers(Map("Authorization" -> TestData.authHeaderWithoutAnyRoles))
    )
    resp.code.code should be(403)
  }

  test("That GET /<article_id> returns 200 if status is allowed even if auth header does not have any roles") {
    when(readService.withId(articleId, lang)).thenReturn(Success(TestData.apiArticleUserTest))

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/$articleId?language=$lang")
        .headers(Map("Authorization" -> TestData.authHeaderWithoutAnyRoles))
    )
    resp.code.code should be(200)

    when(readService.withId(articleId, lang)).thenReturn(
      Success(TestData.apiArticleUserTest.copy(status = api.StatusDTO(EXTERNAL_REVIEW.toString, Seq.empty)))
    )

    val resp2 = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/$articleId?language=$lang")
        .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
    )
    resp2.code.code should be(200)
  }

  test("That GET /<article_id> returns 200 if status is allowed even if no auth header") {
    when(readService.withId(articleId, lang)).thenReturn(Success(TestData.apiArticleUserTest))

    {
      val resp = simpleHttpClient.send(
        quickRequest.get(uri"http://localhost:$serverPort/draft-api/v1/drafts/$articleId?language=$lang")
      )
      resp.code.code should be(200)
    }

    when(readService.withId(articleId, lang)).thenReturn(
      Success(TestData.apiArticleUserTest.copy(status = api.StatusDTO(EXTERNAL_REVIEW.toString, Seq.empty)))
    )

    {
      val resp = simpleHttpClient.send(
        quickRequest
          .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/$articleId?language=$lang")
          .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
      )
      resp.code.code should be(200)
    }
  }

  test("That PATCH /:id returns a validation message if article is invalid") {
    val resp = simpleHttpClient.send(
      quickRequest
        .patch(uri"http://localhost:$serverPort/draft-api/v1/drafts/123")
        .body(invalidArticle)
        .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
    )
    resp.code.code should be(400)
  }

  test("That PATCH /:id returns 403 if access denied") {
    when(writeService.updateArticle(any[Long], any[api.UpdatedArticleDTO], any[TokenUser])).thenReturn(
      Failure(AccessDeniedException("Not today"))
    )

    val resp = simpleHttpClient.send(
      quickRequest
        .patch(uri"http://localhost:$serverPort/draft-api/v1/drafts/123")
        .body(CirceUtil.toJsonString(TestData.sampleApiUpdateArticle))
    )
    resp.code.code should be(401)
  }

  test("That PATCH /:id returns 200 on success") {
    when(writeService.updateArticle(any[Long], any[api.UpdatedArticleDTO], any[TokenUser])).thenReturn(
      Success(TestData.apiArticleWithHtmlFaultV2)
    )
    val resp = simpleHttpClient.send(
      quickRequest
        .patch(uri"http://localhost:$serverPort/draft-api/v1/drafts/123")
        .body(updateTitleJson)
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(200)
  }

  test("PUT /:id/validate/ should return 204 if user has required permissions") {
    when(contentValidator.validateArticleApiArticle(any[Long], any[Boolean], any)).thenReturn(
      Success(api.ContentIdDTO(1))
    )
    val resp = simpleHttpClient.send(
      quickRequest
        .put(uri"http://localhost:$serverPort/draft-api/v1/drafts/1/validate")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(200)
  }

  test("That scrollId is in header, and not in body") {
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse =
      domain.SearchResult[api.ArticleSummaryDTO](0, Some(1), 10, "nb", Seq.empty[api.ArticleSummaryDTO], Some(scrollId))
    when(articleSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchResponse))
    when(searchConverterService.asApiSearchResult(any)).thenCallRealMethod()

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(200)
    resp.body.contains(scrollId) should be(false)
    resp.header("search-context") should be(Some(scrollId))
  }

  test("That scrolling uses scroll and not searches normally") {
    reset(articleSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    when(searchConverterService.asApiSearchResult(any)).thenCallRealMethod()
    val searchResponse =
      domain.SearchResult[api.ArticleSummaryDTO](0, Some(1), 10, "nb", Seq.empty[api.ArticleSummaryDTO], Some(scrollId))

    when(articleSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/?search-context=$scrollId")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )

    resp.code.code should be(200)

    verify(articleSearchService, times(0)).matchingQuery(any[SearchSettings])
    verify(articleSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("That scrolling with POST uses scroll and not searches normally") {
    reset(articleSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse =
      domain.SearchResult[api.ArticleSummaryDTO](0, Some(1), 10, "nb", Seq.empty[api.ArticleSummaryDTO], Some(scrollId))

    when(articleSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))
    when(searchConverterService.asApiSearchResult(any)).thenCallRealMethod()

    val resp = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/draft-api/v1/drafts/search")
        .body(s"""{"scrollId":"$scrollId"}""")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(200)

    verify(articleSearchService, times(0)).matchingQuery(any[SearchSettings])
    verify(articleSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("grepCodes should return 200 OK if the result was not empty") {
    when(readService.getAllGrepCodes(anyString, anyInt, anyInt)).thenReturn(
      Success(TestData.sampleApiGrepCodesSearchResult)
    )

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/grep-codes/")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(200)
  }

  test("grepCodes should return 200 OK if the results are empty") {
    when(readService.getAllGrepCodes(anyString, anyInt, anyInt)).thenReturn(
      Success(TestData.sampleApiGrepCodesSearchResult.copy(results = Seq.empty))
    )

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/grep-codes/")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(200)
  }

  test("tags should return 200 OK if the result was not empty") {
    when(readService.getAllTags(anyString, anyInt, anyInt, anyString)).thenReturn(
      Success(TestData.sampleApiTagsSearchResult)
    )

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/tag-search/")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(200)
  }

  test("tags should return 401 Forbidden if user has no access role") {
    when(readService.getAllTags(anyString, anyInt, anyInt, anyString)).thenReturn(
      Success(TestData.sampleApiTagsSearchResult.copy(results = Seq.empty))
    )

    val resp =
      simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/draft-api/v1/drafts/tag-search/"))
    resp.code.code should be(401)
  }

  test(
    "PATCH / should return 200 on updated, checking json4s deserializer of Either[Null, Option[NewArticleMetaImage]]"
  ) {
    reset(writeService)
    when(writeService.updateArticle(eqTo(1.toLong), any[api.UpdatedArticleDTO], any[TokenUser])).thenReturn(
      Success(TestData.sampleArticleV2)
    )

    val missing         = """{"revision": 1, "language":"nb"}"""
    val missingExpected = TestData.blankUpdatedArticle.copy(language = Some("nb"), metaImage = Missing)

    val nullArtId    = """{"revision": 1, "language":"nb","metaImage":null}"""
    val nullExpected = TestData.blankUpdatedArticle.copy(language = Some("nb"), metaImage = Delete)

    val existingArtId = """{"revision": 1, "language":"nb","metaImage": {"id": "1",
                          |		"alt": "alt-text"}}""".stripMargin
    val existingExpected = TestData
      .blankUpdatedArticle
      .copy(language = Some("nb"), metaImage = UpdateWith(api.NewArticleMetaImageDTO("1", "alt-text")))

    {
      val resp = simpleHttpClient.send(
        quickRequest
          .patch(uri"http://localhost:$serverPort/draft-api/v1/drafts/1")
          .body(missing)
          .headers(Map("Authorization" -> authHeaderWithWriteRole))
      )

      resp.code.code should be(200)
      verify(writeService, times(1)).updateArticle(eqTo(1L), eqTo(missingExpected), any[TokenUser])
    }
    {
      val resp = simpleHttpClient.send(
        quickRequest
          .patch(uri"http://localhost:$serverPort/draft-api/v1/drafts/1")
          .body(nullArtId)
          .headers(Map("Authorization" -> authHeaderWithWriteRole))
      )
      resp.code.code should be(200)

      verify(writeService, times(1)).updateArticle(eqTo(1L), eqTo(nullExpected), any[TokenUser])
    }
    {
      val resp = simpleHttpClient.send(
        quickRequest
          .patch(uri"http://localhost:$serverPort/draft-api/v1/drafts/1")
          .body(existingArtId)
          .headers(Map("Authorization" -> authHeaderWithWriteRole))
      )
      resp.code.code should be(200)
      verify(writeService, times(1)).updateArticle(eqTo(1L), eqTo(existingExpected), any[TokenUser])
    }
  }

  test("that initial search-context doesn't scroll") {
    reset(articleSearchService)

    val expectedSettings = TestData
      .searchSettings
      .copy(
        searchLanguage = "*",
        articleTypes = List("standard", "topic-article", "frontpage-article"),
        shouldScroll = true,
        sort = Sort.ByTitleAsc,
      )

    val result = domain.SearchResult[api.ArticleSummaryDTO](
      totalCount = 0,
      page = None,
      pageSize = 10,
      language = "*",
      results = Seq.empty,
      scrollId = Some("heiheihei"),
    )
    when(articleSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(result))
    when(searchConverterService.asApiSearchResult(any)).thenCallRealMethod()

    val resp = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/drafts/?search-context=initial")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
    )
    resp.code.code should be(200)
    verify(articleSearchService, times(1)).matchingQuery(expectedSettings)
    verify(articleSearchService, times(0)).scroll(any[String], any[String])
  }
}
