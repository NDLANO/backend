/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.common.errors.ValidationException
import no.ndla.common.model.{NDLADate, api => commonApi}
import no.ndla.learningpathapi.TestData.searchSettings
import no.ndla.learningpathapi.integration.Node
import no.ndla.learningpathapi.model.api.{LearningPathSummaryV2, SearchResultV2}
import no.ndla.learningpathapi.model.{api, domain}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.mapping.License.getLicenses
import no.ndla.myndla.model.domain.InvalidStatusException
import no.ndla.network.tapir.auth.TokenUser
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.Serialization._
import org.mockito.ArgumentMatchers._
import org.mockito.Strictness
import org.scalatra.test.scalatest.ScalatraFunSuite

import javax.servlet.http.HttpServletRequest
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

class LearningpathControllerV2Test extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val formats: Formats = org.json4s.DefaultFormats ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer
  implicit val swagger: LearningpathSwagger = new LearningpathSwagger

  val copyright: api.Copyright = api.Copyright(commonApi.License("by-sa", None, None), List())

  val DefaultLearningPathSummary: LearningPathSummaryV2 = api.LearningPathSummaryV2(
    1,
    None,
    api.Title("Tittel", "nb"),
    api.Description("", "nb"),
    api.Introduction("", "nb"),
    "",
    None,
    None,
    "",
    NDLADate.now(),
    api.LearningPathTags(Seq(), "nb"),
    copyright,
    List("nb"),
    None,
    None
  )

  lazy val controller = new LearningpathControllerV2
  addServlet(controller, "/*")

  override def beforeEach(): Unit = {
    resetMocks()
    when(languageValidator.validate(any[String], any[String], any[Boolean]))
      .thenReturn(None)
  }

  test("That GET / will send all query-params to the search service") {
    val query              = "hoppetau"
    val tag                = "lek"
    val language           = "nb"
    val page               = 22
    val pageSize           = 111
    val ids                = "1,2"
    val verificationStatus = "EXTERNAL"

    val result    = SearchResult(1, Some(1), 1, language, Seq(DefaultLearningPathSummary), None)
    val apiResult = SearchResultV2(1, Some(1), 1, language, Seq(DefaultLearningPathSummary))
    when(searchConverterService.asApiSearchResult(result)).thenReturn(apiResult)

    val expectedSettings = searchSettings.copy(
      query = Some(query),
      withIdIn = List(1, 2),
      taggedWith = Some(tag),
      language = Some(language),
      sort = Sort.ByDurationDesc,
      page = Some(page),
      pageSize = Some(pageSize),
      verificationStatus = Some(verificationStatus)
    )

    when(searchService.matchingQuery(eqTo(expectedSettings))).thenReturn(Success(result))

    get(
      "/",
      Map(
        "query"              -> query,
        "tag"                -> tag,
        "language"           -> language,
        "sort"               -> "-duration",
        "page-size"          -> s"$pageSize",
        "page"               -> s"$page",
        "ids"                -> s"$ids",
        "verificationStatus" -> s"$verificationStatus"
      )
    ) {
      status should equal(200)
      val convertedBody = read[api.SearchResultV2](body)
      convertedBody.results.head.title should equal(api.Title("Tittel", "nb"))
    }
  }

  test("That GET / will handle all empty query-params as missing query params") {
    val query    = ""
    val tag      = ""
    val language = ""
    val page     = ""
    val pageSize = ""
    val duration = ""
    val ids      = "1,2"

    val result    = SearchResult(-1, Some(1), 1, "nb", Seq(DefaultLearningPathSummary), None)
    val apiResult = SearchResultV2(-1, Some(1), 1, "nb", Seq(DefaultLearningPathSummary))
    when(searchConverterService.asApiSearchResult(result)).thenReturn(apiResult)

    when(searchService.matchingQuery(any[SearchSettings])).thenReturn(Success(result))

    get(
      "/",
      Map(
        "query"     -> query,
        "tag"       -> tag,
        "language"  -> language,
        "sort"      -> duration,
        "page-size" -> s"$pageSize",
        "page"      -> s"$page",
        "ids"       -> s"$ids"
      )
    ) {
      status should equal(200)
      val convertedBody = read[api.SearchResultV2](body)
      convertedBody.totalCount should be(-1)
    }

  }

  test("That POST /search will send all query-params to the search service") {
    val query    = "hoppetau"
    val tag      = "lek"
    val language = "nb"
    val page     = 22
    val pageSize = 111

    val result    = SearchResult(1, Some(page), pageSize, language, Seq(DefaultLearningPathSummary), None)
    val apiResult = SearchResultV2(1, Some(page), pageSize, language, Seq(DefaultLearningPathSummary))
    when(searchConverterService.asApiSearchResult(result)).thenReturn(apiResult)

    val expectedSettings = searchSettings.copy(
      withIdIn = List(1, 2),
      query = Some(query),
      taggedWith = Some(tag),
      language = Some(language),
      sort = Sort.ByDurationDesc,
      page = Some(page),
      pageSize = Some(pageSize)
    )

    when(searchService.matchingQuery(eqTo(expectedSettings))).thenReturn(Success(result))

    post(
      "/search/",
      body =
        s"""{"query": "$query", "tag": "$tag", "language": "$language", "page": $page, "pageSize": $pageSize, "ids": [1, 2], "sort": "-duration" }"""
    ) {
      status should equal(200)
      val convertedBody = read[api.SearchResultV2](body)
      convertedBody.results.head.title should equal(api.Title("Tittel", "nb"))
    }
  }

  test("That GET /licenses with filter sat to by only returns creative common licenses") {
    val creativeCommonlicenses = getLicenses
      .filter(_.license.toString.startsWith("by"))
      .map(l => commonApi.License(l.license.toString, Option(l.description), l.url))
      .toSet

    get(
      "/licenses/",
      Map(
        "filter" -> "by"
      )
    ) {
      status should equal(200)
      val convertedBody = read[Set[commonApi.License]](body)
      convertedBody should equal(creativeCommonlicenses)
    }
  }

  test("That GET /licenses with filter not specified returns all licenses") {
    val allLicenses = getLicenses
      .map(l => commonApi.License(l.license.toString, Option(l.description), l.url))
      .toSet

    get("/licenses/", Map()) {
      status should equal(200)
      val convertedBody = read[Set[commonApi.License]](body)
      convertedBody should equal(allLicenses)
    }
  }

  test("That paramAsListOfLong returns empty list when empty param") {
    implicit val request: HttpServletRequest = mock[HttpServletRequest](withSettings.strictness(Strictness.Lenient))
    val paramName                            = "test"
    val parameterMap                         = Map("someOther" -> Array(""))

    when(request.getParameterMap).thenReturn(parameterMap.asJava)
    controller.paramAsListOfLong(paramName)(request) should equal(List())
  }

  test("That paramAsListOfLong returns List of longs for all ids specified in input") {
    implicit val request: HttpServletRequest = mock[HttpServletRequest](withSettings.strictness(Strictness.Lenient))
    val expectedList                         = List(1, 2, 3, 5, 6, 7, 8)
    val paramName                            = "test"
    val parameterMap                         = Map(paramName -> Array(expectedList.mkString(" , ")))

    when(request.getParameterMap).thenReturn(parameterMap.asJava)
    controller.paramAsListOfLong(paramName)(request) should equal(expectedList)
  }

  test("That paramAsListOfLong returns validation error when list of ids contains a string") {
    implicit val request: HttpServletRequest = mock[HttpServletRequest](withSettings.strictness(Strictness.Lenient))
    val paramName                            = "test"
    val parameterMap                         = Map(paramName -> Array("1,2,abc,3"))

    when(request.getParameterMap).thenReturn(parameterMap.asJava)

    val validationException = intercept[ValidationException] {
      controller.paramAsListOfLong(paramName)(request)
    }

    validationException.errors.size should be(1)
    validationException.errors.head.field should equal(paramName)
    validationException.errors.head.message should equal(
      s"Invalid value for $paramName. Only (list of) digits are allowed."
    )

  }

  test("That /with-status returns 400 if invalid status is specified") {
    when(readService.learningPathWithStatus(any[String], any[TokenUser]))
      .thenReturn(Failure(InvalidStatusException("Bad status")))

    get("/status/invalidStatusHurrDurr") {
      status should equal(400)
    }

    when(readService.learningPathWithStatus(any[String], any[TokenUser]))
      .thenReturn(Success(List.empty))
    get("/status/unlisted") {
      status should equal(200)
    }

  }

  test("That scrollId is in header, and not in body") {
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult(
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )
    when(searchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchResponse))

    get(s"/") {
      status should be(200)
      body.contains(scrollId) should be(false)
      response.getHeader("search-context") should be(scrollId)
    }
  }

  test("That scrolling uses scroll and not searches normally") {
    reset(searchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult(
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )

    when(searchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))

    get(s"/?search-context=$scrollId") {
      status should be(200)
    }

    verify(searchService, times(0)).matchingQuery(any[SearchSettings])
    verify(searchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("That scrolling with POST uses scroll and not searches normally") {
    reset(searchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult(
      0,
      Some(1),
      10,
      "nb",
      Seq.empty,
      Some(scrollId)
    )

    when(searchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))

    post(s"/search/", body = s"""{"scrollId":"$scrollId"}""") {
      status should be(200)
    }

    verify(searchService, times(0)).matchingQuery(any[SearchSettings])
    verify(searchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

  test("that initial search-context doesn't scroll") {
    reset(searchService)

    val expectedSettings = TestData.searchSettings.copy(
      language = Some("*"),
      shouldScroll = true,
      sort = Sort.ByTitleAsc
    )

    val result = domain.SearchResult(
      totalCount = 0,
      page = None,
      pageSize = 10,
      language = "all",
      results = Seq.empty,
      scrollId = Some("heiheihei")
    )
    when(searchService.matchingQuery(any[SearchSettings])).thenReturn(Success(result))

    get("/?search-context=initial") {
      status should be(200)
      verify(searchService, times(1)).matchingQuery(expectedSettings)
      verify(searchService, times(0)).scroll(any[String], any[String])
    }
  }

  test("That GET /contains-article returns 200") {
    reset(taxonomyApiClient)

    val result = domain.SearchResult(
      totalCount = 0,
      page = None,
      pageSize = 10,
      language = "all",
      results = Seq.empty,
      scrollId = Some("heiheihei")
    )
    when(taxonomyApiClient.queryNodes(any[Long])).thenReturn(Success(List[Node]()))
    when(searchService.containsPath(any[List[String]])).thenReturn(Success(result))

    get("/contains-article/123") {
      status should be(200)
    }
  }

  test("That GET /contains-article returns correct errors when id is a string or nothing") {
    reset(taxonomyApiClient)

    get("/contains-article/hallohallo") {
      status should be(400)
    }

    get("/contains-article/") {
      status should be(404)
    }
  }
}
