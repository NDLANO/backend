/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.model.{api, domain}
import no.ndla.articleapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.common.model.domain.Availability
import no.ndla.tapirtesting.TapirControllerTest
import org.json4s.ext.EnumNameSerializer
import org.json4s.{DefaultFormats, Formats}
import org.mockito.ArgumentMatchers._
import sttp.client3.quick._

import scala.util.{Failure, Success}

class ArticleControllerV2Test extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {

  val legacyAuthHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsiYXJ0aWNsZXM6d3JpdGUiXSwibmRsYV9pZCI6ImFiYzEyMyJ9LCJuYW1lIjoiRG9uYWxkIER1Y2siLCJpc3MiOiJodHRwczovL3NvbWUtZG9tYWluLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTIzIiwiYXVkIjoiYWJjIiwiZXhwIjoxNDg2MDcwMDYzLCJpYXQiOjE0ODYwMzQwNjN9.VxqM2bu2UF8IAalibIgdRdmsTDDWKEYpKzHPbCJcFzA"

  val legacyAuthHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOltdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30.kXjaQ9QudcRHTqhfrzKr0Zr4pYISBfJoXWHVBreDyO8"

  val legacyAuthHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsic29tZTpvdGhlciJdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30.JsxMW8y0hCmpuu9tpQr6ZdfcqkOS8hRatFi3cTO_PvY"

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXJ0aWNsZXM6d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.kh82qM84FZgoo3odWbHTLWy-N049m7SyQw4gdatDMk43H2nWHA6gjsbJoiBIZ7BcbSfHElEZH0tP94vRy-kjgA3hflhOBbsD73DIxRvnbH1kSXlBnl6ISbgtHnzv1wQ7ShykMAcBsoWQ6J16ixK_p-msW42kcEqK1LanzPy-_qI"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.fb9eTuBwIlbGDgDKBQ5FVpuSUdgDVBZjCenkOrWLzUByVCcaFhbFU8CVTWWKhKJqt6u-09-99hh86szURLqwl3F5rxSX9PrnbyhI9LsPut_3fr6vezs6592jPJRbdBz3-xLN0XY5HIiJElJD3Wb52obTqJCrMAKLZ5x_GLKGhcY"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoic29tZTpvdGhlciIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.Hbmh9KX19nx7yT3rEcP9pyzRO0uQJBRucfqH9QEZtLyXjYj_fAyOhsoicOVEbHSES7rtdiJK43-gijSpWWmGWOkE6Ym7nHGhB_nLdvp_25PDgdKHo-KawZdAyIcJFr5_t3CJ2Z2IPVbrXwUd99vuXEBaV0dMwkT0kDtkwHuS-8E"

  implicit val formats: Formats = DefaultFormats + new EnumNameSerializer(Availability)

  lazy val controller = new ArticleControllerV2

  override def beforeEach(): Unit = {
    reset(clock, searchConverterService)
    when(clock.now()).thenCallRealMethod()
  }

  val updateTitleJson = """{"revision": 1, "title": "hehe", "language": "nb", "content": "content"}"""
  val invalidArticle  = """{"revision": 1, "title": [{"language": "nb", "titlee": "lol"]}"""
  val lang            = "nb"
  val articleId       = 1L

  test("/<article_id> should return 200 if the cover was found withIdV2") {
    when(readService.withIdV2(articleId, lang, fallback = false, None, None))
      .thenReturn(Success(domain.Cachable.yes(TestData.sampleArticleV2)))

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/$articleId?language=$lang")
      )
      .code
      .code should be(200)
  }

  test("/<article_id> should return 404 if the article was not found withIdV2") {
    when(readService.withIdV2(articleId, lang, fallback = false, None, None))
      .thenReturn(Failure(api.NotFoundException("Not found")))

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/$articleId?language=$lang")
      )
      .code
      .code should be(404)
  }

  test("/<article_id> should return 200 if parameter is correctly formatted (urn:article:<id>#<revision>)") {
    val articleId2             = 23L
    val revision               = 5
    val articleUrnWithRevision = s"urn:article:$articleId2#$revision"

    when(
      readService.withIdV2(
        articleId2,
        "*",
        fallback = false,
        Some(revision),
        None
      )
    ).thenReturn(Success(domain.Cachable.yes(TestData.sampleArticleV2)))

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/$articleUrnWithRevision")
      )
      .code
      .code should be(200)
  }

  test("/<article_id> should return 200 if slug was sent as parameter") {
    val slug = "someslug"

    when(readService.getArticleBySlug(any, any, any))
      .thenReturn(Success(domain.Cachable.yes(TestData.sampleArticleV2)))
    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/$slug")
      )
      .code
      .code should be(200)
  }

  test("/<article_id> default behavior should be to find by slug") {
    val malformedUrn = s"urn:article:malformed#hue"

    when(readService.getArticleBySlug(any, any, any))
      .thenReturn(Failure(api.NotFoundException("Not found")))
    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/$malformedUrn")
      )
      .code
      .code should be(404)
  }

  test("That scrollId is in header, and not in body") {
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult[api.ArticleSummaryV2](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[api.ArticleSummaryV2],
      Some(scrollId)
    )
    when(readService.search(any, any, any, any, any, any, any, any, any, any, any, any))
      .thenReturn(Success(domain.Cachable.yes(searchResponse)))
    when(searchConverterService.asApiSearchResultV2(any)).thenCallRealMethod()

    val resp = simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/")
      )

    resp.code.code should be(200)
    resp.body.contains(scrollId) should be(false)
    resp.header("search-context") should be(Some(scrollId))
  }

  test("That scrolling uses scroll and not searches normally") {
    reset(articleSearchService, readService)
    when(searchConverterService.asApiSearchResultV2(any)).thenCallRealMethod()
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult[api.ArticleSummaryV2](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[api.ArticleSummaryV2],
      Some(scrollId)
    )

    when(articleSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))

    val resp = simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/article-api/v2/articles/?search-context=$scrollId")
      )
    resp.code.code should be(200)

    verify(articleSearchService, times(0)).matchingQuery(any[domain.SearchSettings])
    verify(readService, times(0)).search(any, any, any, any, any, any, any, any, any, any, any, any)
    verify(articleSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("That scrolling with POST uses scroll and not searches normally") {
    reset(articleSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult[api.ArticleSummaryV2](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[api.ArticleSummaryV2],
      Some(scrollId)
    )

    when(articleSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))
    when(searchConverterService.asApiSearchResultV2(any)).thenCallRealMethod()

    val response = simpleHttpClient
      .send(
        quickRequest
          .post(uri"http://localhost:$serverPort/article-api/v2/articles/search")
          .body(s"""{"scrollId":"$scrollId"}""")
          .header("content-type", "application/json")
      )
    response.code.code should be(200)

    verify(articleSearchService, times(0)).matchingQuery(any[domain.SearchSettings])
    verify(articleSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("tags should return 200 OK if the result was not empty") {
    when(readService.getAllTags(anyString, anyInt, anyInt, anyString))
      .thenReturn(TestData.sampleApiTagsSearchResult)

    val response = simpleHttpClient
      .send(
        quickRequest
          .get(uri"http://localhost:$serverPort/article-api/v2/articles/tag-search/")
      )
    response.code.code should be(200)
  }

  test("That initial search-context doesn't scroll") {
    reset(articleSearchService, readService)

    val result = SearchResult[api.ArticleSummaryV2](
      totalCount = 0,
      page = None,
      pageSize = 10,
      language = "*",
      results = Seq.empty,
      scrollId = Some("heiheihei")
    )
    when(readService.search(any, any, any, any, any, any, any, any, any, any, any, any))
      .thenReturn(Success(domain.Cachable.yes(result)))
    when(searchConverterService.asApiSearchResultV2(any)).thenCallRealMethod()
    simpleHttpClient
      .send(
        quickRequest
          .get(uri"http://localhost:$serverPort/article-api/v2/articles/?search-context=initial")
      )
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
      feideAccessToken = any
    )
    verify(articleSearchService, times(0)).scroll(any[String], any[String])
  }

  test("that /ids/ works, and isnt a slug") {
    reset(readService)
    when(readService.getArticlesByIds(any, any, any, any, any, any)).thenReturn(Success(Seq.empty))

    val response = simpleHttpClient
      .send(
        quickRequest
          .get(uri"http://localhost:$serverPort/article-api/v2/articles/ids/?ids=1,2,3")
      )
    verify(readService, times(1)).getArticlesByIds(eqTo(List(1L, 2L, 3L)), any, any, any, any, any)
    verify(readService, never).getArticleBySlug(any, any, any)
    verify(readService, never).withIdV2(any, any, any, any, any)
    response.code.code should be(200)
  }

}
