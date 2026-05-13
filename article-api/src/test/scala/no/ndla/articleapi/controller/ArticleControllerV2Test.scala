/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.model.{NotFoundException, api, domain}
import no.ndla.articleapi.{TestEnvironment, UnitSuite, model}
import no.ndla.common.Clock
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.{never, reset, times, verify, when}
import sttp.client4.quick.*

import scala.util.{Failure, Success}

class ArticleControllerV2Test extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock: Clock                    = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  override val controller: ArticleControllerV2               = new ArticleControllerV2
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes

  override def beforeEach(): Unit = {
    reset(clock)
    reset(searchConverterService)
    when(clock.now()).thenCallRealMethod()
  }

  val updateTitleJson = """{"revision": 1, "title": "hehe", "language": "nb", "content": "content"}"""
  val invalidArticle  = """{"revision": 1, "title": [{"language": "nb", "titlee": "lol"]}"""
  val lang            = "nb"
  val articleId       = 1L

  test("/<article_id> should return 200 if the cover was found withIdV2") {
    when(readService.withIdV2(articleId, lang, fallback = false, None, None)).thenReturn(
      Success(domain.Cachable.yes(TestData.sampleArticleV2))
    )

    quickRequest
      .get(uri"http://localhost:$serverPort/article-api/v2/articles/$articleId?language=$lang")
      .send()
      .code
      .code should be(200)
  }

  test("/<article_id> should return 404 if the article was not found withIdV2") {
    when(readService.withIdV2(articleId, lang, fallback = false, None, None)).thenReturn(
      Failure(NotFoundException("Not found"))
    )

    quickRequest
      .get(uri"http://localhost:$serverPort/article-api/v2/articles/$articleId?language=$lang")
      .send()
      .code
      .code should be(404)
  }

  test("/<article_id> should return 200 if parameter is correctly formatted (urn:article:<id>#<revision>)") {
    val articleId2             = 23L
    val revision               = 5
    val articleUrnWithRevision = s"urn:article:$articleId2#$revision"

    when(readService.withIdV2(articleId2, "*", fallback = false, Some(revision), None)).thenReturn(
      Success(domain.Cachable.yes(TestData.sampleArticleV2))
    )

    quickRequest
      .get(uri"http://localhost:$serverPort/article-api/v2/articles/$articleUrnWithRevision")
      .send()
      .code
      .code should be(200)
  }

  test("/<article_id> should return 200 if slug was sent as parameter") {
    val slug = "someslug"

    when(readService.getArticleBySlug(any, any, any)).thenReturn(Success(domain.Cachable.yes(TestData.sampleArticleV2)))
    quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/$slug").send().code.code should be(200)
  }

  test("/<article_id> default behavior should be to find by slug") {
    val malformedUrn = s"urn:article:malformed#hue"

    when(readService.getArticleBySlug(any, any, any)).thenReturn(Failure(model.NotFoundException("Not found")))
    quickRequest
      .get(uri"http://localhost:$serverPort/article-api/v2/articles/$malformedUrn")
      .send()
      .code
      .code should be(404)
  }

  test("That scrollId is in header, and not in body") {
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse =
      SearchResult[api.ArticleSummaryV2DTO](0, Some(1), 10, "nb", Seq.empty[api.ArticleSummaryV2DTO], Some(scrollId))
    when(readService.search(any, any, any, any, any, any, any, any, any, any, any, any)).thenReturn(
      Success(domain.Cachable.yes(searchResponse))
    )
    when(searchConverterService.asApiSearchResultV2(any)).thenCallRealMethod()

    val resp = quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/").send()

    resp.code.code should be(200)
    resp.body.contains(scrollId) should be(false)
    resp.header("search-context") should be(Some(scrollId))
  }

  test("That scrolling uses scroll and not searches normally") {
    reset(articleSearchService, readService)
    when(searchConverterService.asApiSearchResultV2(any)).thenCallRealMethod()
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse =
      SearchResult[api.ArticleSummaryV2DTO](0, Some(1), 10, "nb", Seq.empty[api.ArticleSummaryV2DTO], Some(scrollId))

    when(articleSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))

    val resp = quickRequest
      .get(uri"http://localhost:$serverPort/article-api/v2/articles/?search-context=$scrollId")
      .send()
    resp.code.code should be(200)

    verify(articleSearchService, times(0)).matchingQuery(any[domain.SearchSettings])
    verify(readService, times(0)).search(any, any, any, any, any, any, any, any, any, any, any, any)
    verify(articleSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("That scrolling with POST uses scroll and not searches normally") {
    reset(articleSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse =
      SearchResult[api.ArticleSummaryV2DTO](0, Some(1), 10, "nb", Seq.empty[api.ArticleSummaryV2DTO], Some(scrollId))

    when(articleSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))
    when(searchConverterService.asApiSearchResultV2(any)).thenCallRealMethod()

    val response = quickRequest
      .post(uri"http://localhost:$serverPort/article-api/v2/articles/search")
      .body(s"""{"scrollId":"$scrollId"}""")
      .header("content-type", "application/json")
      .send()
    response.code.code should be(200)

    verify(articleSearchService, times(0)).matchingQuery(any[domain.SearchSettings])
    verify(articleSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("tags should return 200 OK if the result was not empty") {
    when(readService.getAllTags(anyString, anyInt, anyInt, anyString)).thenReturn(
      Success(TestData.sampleApiTagsSearchResult)
    )

    val response = quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/tag-search/").send()
    response.code.code should be(200)
  }

  test("That initial search-context doesn't scroll") {
    reset(articleSearchService, readService)

    val result = SearchResult[api.ArticleSummaryV2DTO](
      totalCount = 0,
      page = None,
      pageSize = 10,
      language = "*",
      results = Seq.empty,
      scrollId = Some("heiheihei"),
    )
    when(readService.search(any, any, any, any, any, any, any, any, any, any, any, any)).thenReturn(
      Success(domain.Cachable.yes(result))
    )
    when(searchConverterService.asApiSearchResultV2(any)).thenCallRealMethod()
    quickRequest
      .get(uri"http://localhost:$serverPort/article-api/v2/articles/?search-context=initial")
      .send()
      .code
      .code should be(200)

    verify(readService, times(1)).search(
      query = any,
      sort = any,
      language = eqTo("*"),
      license = any,
      page = any,
      pageSize = any,
      idList = any,
      articleTypesFilter = any,
      fallback = any,
      grepCodes = any,
      shouldScroll = eqTo(true),
      feide = any,
    )
    verify(articleSearchService, times(0)).scroll(any[String], any[String])
  }

  test("that /ids/ works, and isnt a slug") {
    reset(readService)
    when(readService.getArticlesByIds(any, any, any, any, any, any)).thenReturn(Success(Seq.empty))

    val response = quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/ids/?ids=1,2,3").send()
    verify(readService, times(1)).getArticlesByIds(eqTo(List(1L, 2L, 3L)), any, any, any, any, any)
    verify(readService, never).getArticleBySlug(any, any, any)
    verify(readService, never).withIdV2(any, any, any, any, any)
    response.code.code should be(200)
  }

}
