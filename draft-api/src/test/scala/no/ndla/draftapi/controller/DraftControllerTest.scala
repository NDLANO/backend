/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.DraftApiProperties.DefaultLanguage

import java.util.Date
import no.ndla.draftapi.TestData.authHeaderWithWriteRole
import no.ndla.draftapi.auth.UserInfo
import no.ndla.draftapi.model.api._
import no.ndla.draftapi.model.domain.ArticleStatus.{QUALITY_ASSURED_DELAYED, QUEUED_FOR_PUBLISHING_DELAYED}
import no.ndla.draftapi.model.domain.{ArticleType, Language, SearchSettings, Sort}
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.{DraftSwagger, TestData, TestEnvironment, UnitSuite}
import no.ndla.mapping.License.getLicenses
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.{read, write}
import org.mockito.ArgumentMatchers._
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.{Failure, Success}

class DraftControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  implicit val swagger: DraftSwagger = new DraftSwagger

  lazy val controller = new DraftController
  addServlet(controller, "/test")

  val updateTitleJson = """{"revision": 1, "title": "hehe", "language": "nb", "content": "content"}"""
  val invalidArticle = """{"revision": 1, "title": [{"language": "nb", "titlee": "lol"]}""" // typo in "titlee"
  val invalidNewArticle = """{ "language": "nb", "content": "<section><h2>Hi</h2></section>" }""" // missing title
  val lang = "nb"
  val articleId = 1

  override def beforeEach(): Unit = {
    when(user.getUser).thenReturn(TestData.userWithWriteAccess)
  }

  test("/<article_id> should return 200 if the cover was found withId") {
    when(readService.withId(articleId, lang)).thenReturn(Success(TestData.sampleArticleV2))

    get(s"/test/$articleId?language=$lang") {
      status should equal(200)
    }
  }

  test("/<article_id> should return 404 if the article was not found withId") {
    when(readService.withId(articleId, lang)).thenReturn(Failure(NotFoundException("not found yo")))

    get(s"/test/$articleId?language=$lang") {
      status should equal(404)
    }
  }

  test("/<article_id> should return 400 if the article was not found withId") {
    get(s"/test/one") {
      status should equal(400)
    }
  }

  test("That GET /licenses/ with filter sat to by only returns creative common licenses") {
    val creativeCommonlicenses =
      getLicenses
        .filter(_.license.toString.startsWith("by"))
        .map(l => License(l.license.toString, Option(l.description), l.url))
        .toSet

    get("/test/licenses/", "filter" -> "by") {
      status should equal(200)
      val convertedBody = read[Set[License]](body)
      convertedBody should equal(creativeCommonlicenses)
    }
  }

  test("That GET /licenses/ with filter not specified returns all licenses") {
    val allLicenses = getLicenses.map(l => License(l.license.toString, Option(l.description), l.url)).toSet

    get("/test/licenses/") {
      status should equal(200)
      val convertedBody = read[Set[License]](body)
      convertedBody should equal(allLicenses)
    }
  }

  test("GET / should use size of id-list as page-size if defined") {
    val searchMock = mock[domain.SearchResult[api.ArticleSummary]]
    when(searchMock.scrollId).thenReturn(None)
    when(articleSearchService.matchingQuery(any[SearchSettings]))
      .thenReturn(Success(searchMock))

    get("/test/", "ids" -> "1,2,3,4", "page-size" -> "10", "language" -> "nb") {
      status should equal(200)

      verify(articleSearchService, times(1)).matchingQuery(
        TestData.searchSettings.copy(
          withIdIn = List(1, 2, 3, 4),
          searchLanguage = DefaultLanguage,
          page = 1,
          pageSize = 4,
          sort = Sort.ByTitleAsc,
          articleTypes = ArticleType.all
        ))
    }
  }

  test("POST / should return 400 if body does not contain all required fields") {
    post("/test/", invalidArticle) {
      status should equal(400)
    }
  }

  test("POST / should return 201 on created") {
    when(
      writeService
        .newArticle(any[NewArticle],
                    any[List[String]],
                    any[Seq[String]],
                    any[UserInfo],
                    any[Option[Date]],
                    any[Option[Date]],
                    any[Option[String]]))
      .thenReturn(Success(TestData.sampleArticleV2))
    post("/test/", write(TestData.newArticle), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(201)
    }
  }

  test("That / returns a validation message if article is invalid") {
    post("/test") {
      status should equal(400)
    }
  }

  test("That POST / returns 403 if no auth-header") {
    when(user.getUser).thenReturn(TestData.userWithNoRoles)
    post("/test") {
      status should equal(403)
    }
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    when(user.getUser).thenReturn(TestData.userWithNoRoles)

    post("/test") {
      status should equal(403)
    }
  }

  test("That GET /<article_id> returns 403 if auth header does not have any roles") {
    when(user.getUser).thenReturn(TestData.userWithNoRoles)
    when(readService.withId(articleId, lang)).thenReturn(Success(TestData.sampleArticleV2))

    get(s"/test/$articleId?language=$lang") {
      status should equal(403)
    }
  }

  test("That GET /<article_id> returns 200 if status is allowed even if auth header does not have any roles") {
    when(user.getUser).thenReturn(TestData.userWithNoRoles)
    when(readService.withId(articleId, lang)).thenReturn(Success(TestData.apiArticleUserTest))

    get(s"/test/$articleId?language=$lang") {
      status should equal(200)
    }

    when(readService.withId(articleId, lang)).thenReturn(
      Success(TestData.apiArticleUserTest.copy(status = api.Status(QUALITY_ASSURED_DELAYED.toString, Seq.empty))))

    get(s"/test/$articleId?language=$lang") {
      status should equal(200)
    }

    when(readService.withId(articleId, lang)).thenReturn(
      Success(TestData.apiArticleUserTest.copy(status = api.Status(QUEUED_FOR_PUBLISHING_DELAYED.toString, Seq.empty))))

    get(s"/test/$articleId?language=$lang") {
      status should equal(200)
    }
  }

  test("That PATCH /:id returns a validation message if article is invalid") {
    patch("/test/123", invalidArticle, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("That PATCH /:id returns 403 if access denied") {
    when(user.getUser).thenReturn(TestData.userWithNoRoles)
    when(
      writeService.updateArticle(
        any[Long],
        any[api.UpdatedArticle],
        any[List[String]],
        any[Seq[String]],
        any[UserInfo],
        any[Option[Date]],
        any[Option[Date]],
        any[Option[String]]
      ))
      .thenReturn(Failure(new AccessDeniedException("Not today")))

    patch("/test/123", body = write(TestData.sampleApiUpdateArticle)) {
      status should equal(403)
    }
  }

  test("That PATCH /:id returns 200 on success") {
    when(
      writeService.updateArticle(any[Long],
                                 any[UpdatedArticle],
                                 any[List[String]],
                                 any[Seq[String]],
                                 any[UserInfo],
                                 any[Option[Date]],
                                 any[Option[Date]],
                                 any[Option[String]]))
      .thenReturn(Success(TestData.apiArticleWithHtmlFaultV2))
    patch("/test/123", updateTitleJson) {
      status should equal(200)
    }
  }

  test("PUT /:id/validate/ should return 204 if user has required permissions") {
    when(contentValidator.validateArticleApiArticle(any[Long], any[Boolean])).thenReturn(Success(ContentId(1)))
    put("/test/1/validate/") {
      status should equal(200)
    }
  }

  test("That scrollId is in header, and not in body") {
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[api.ArticleSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[api.ArticleSummary],
      Some(scrollId)
    )
    when(articleSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchResponse))

    get(s"/test/") {
      status should be(200)
      body.contains(scrollId) should be(false)
      header("search-context") should be(scrollId)
    }
  }

  test("That scrolling uses scroll and not searches normally") {
    reset(articleSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[api.ArticleSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[api.ArticleSummary],
      Some(scrollId)
    )

    when(articleSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))

    get(s"/test?search-context=$scrollId") {
      status should be(200)
    }

    verify(articleSearchService, times(0)).matchingQuery(any[SearchSettings])
    verify(articleSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("That scrolling with POST uses scroll and not searches normally") {
    reset(articleSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = domain.SearchResult[api.ArticleSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[api.ArticleSummary],
      Some(scrollId)
    )

    when(articleSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))

    post(s"/test/search/", body = s"""{"scrollId": "$scrollId"}""") {
      status should be(200)
    }

    verify(articleSearchService, times(0)).matchingQuery(any[SearchSettings])
    verify(articleSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("grepCodes should return 200 OK if the result was not empty") {
    when(readService.getAllGrepCodes(anyString, anyInt, anyInt))
      .thenReturn(Success(TestData.sampleApiGrepCodesSearchResult))

    get("/test/grep-codes/") {
      status should equal(200)
    }
  }

  test("grepCodes should return 200 OK if the results are empty") {
    when(readService.getAllGrepCodes(anyString, anyInt, anyInt))
      .thenReturn(Success(TestData.sampleApiGrepCodesSearchResult.copy(results = Seq.empty)))

    get("/test/grep-codes/") {
      status should equal(200)
    }
  }

  test("tags should return 200 OK if the result was not empty") {
    when(readService.getAllTags(anyString, anyInt, anyInt, anyString))
      .thenReturn(Success(TestData.sampleApiTagsSearchResult))

    get("/test/tag-search/") {
      status should equal(200)
    }
  }

  test("tags should return 403 Forbidden if user has no access role") {
    when(user.getUser).thenReturn(TestData.userWithNoRoles)
    when(readService.getAllTags(anyString, anyInt, anyInt, anyString))
      .thenReturn(Success(TestData.sampleApiTagsSearchResult.copy(results = Seq.empty)))

    get("/test/tag-search/") {
      status should equal(403)
    }
  }

  test(
    "PATCH / should return 200 on updated, checking json4s deserializer of Either[Null, Option[NewArticleMetaImage]]") {
    reset(writeService)
    when(
      writeService
        .updateArticle(eqTo(1.toLong),
                       any[UpdatedArticle],
                       any[List[String]],
                       any[Seq[String]],
                       any[UserInfo],
                       any[Option[Date]],
                       any[Option[Date]],
                       any[Option[String]]))
      .thenReturn(Success(TestData.sampleArticleV2))

    val missing = """{"revision": 1, "language":"nb"}"""
    val missingExpected = TestData.blankUpdatedArticle.copy(language = Some("nb"), metaImage = Right(None))

    val nullArtId = """{"revision": 1, "language":"nb","metaImage":null}"""
    val nullExpected = TestData.blankUpdatedArticle.copy(language = Some("nb"), metaImage = Left(null))

    val existingArtId = """{"revision": 1, "language":"nb","metaImage": {"id": "1",
                          |		"alt": "alt-text"}}""".stripMargin
    val existingExpected = TestData.blankUpdatedArticle
      .copy(language = Some("nb"), metaImage = Right(Some(api.NewArticleMetaImage("1", "alt-text"))))

    patch("/test/1", missing, headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(200)
      verify(writeService, times(1)).updateArticle(eqTo(1),
                                                   eqTo(missingExpected),
                                                   any[List[String]],
                                                   any[Seq[String]],
                                                   any[UserInfo],
                                                   any[Option[Date]],
                                                   any[Option[Date]],
                                                   any[Option[String]])
    }

    patch("/test/1", nullArtId, headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(200)
      verify(writeService, times(1)).updateArticle(eqTo(1),
                                                   eqTo(nullExpected),
                                                   any[List[String]],
                                                   any[Seq[String]],
                                                   any[UserInfo],
                                                   any[Option[Date]],
                                                   any[Option[Date]],
                                                   any[Option[String]])
    }

    patch("/test/1", existingArtId, headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(200)
      verify(writeService, times(1)).updateArticle(eqTo(1),
                                                   eqTo(existingExpected),
                                                   any[List[String]],
                                                   any[Seq[String]],
                                                   any[UserInfo],
                                                   any[Option[Date]],
                                                   any[Option[Date]],
                                                   any[Option[String]])
    }
  }

  test("that initial search-context doesn't scroll") {
    reset(articleSearchService)

    val expectedSettings = TestData.searchSettings.copy(
      searchLanguage = "*",
      articleTypes = List("standard", "topic-article"),
      shouldScroll = true,
      sort = Sort.ByTitleAsc
    )

    val result = domain.SearchResult[api.ArticleSummary](
      totalCount = 0,
      page = None,
      pageSize = 10,
      language = "*",
      results = Seq.empty,
      scrollId = Some("heiheihei")
    )
    when(articleSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(result))

    get("/test/?search-context=initial") {
      status should be(200)
      verify(articleSearchService, times(1)).matchingQuery(expectedSettings)
      verify(articleSearchService, times(0)).scroll(any[String], any[String])
    }

  }
}
