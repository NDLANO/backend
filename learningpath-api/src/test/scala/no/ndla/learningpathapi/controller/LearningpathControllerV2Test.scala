/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.common.CirceUtil
import no.ndla.common.model.{NDLADate, api as commonApi}
import no.ndla.learningpathapi.TestData.searchSettings
import no.ndla.learningpathapi.integration.Node
import no.ndla.learningpathapi.model.api.{LearningPathSummaryV2DTO, SearchResultV2DTO}
import no.ndla.learningpathapi.model.domain.*
import no.ndla.learningpathapi.model.{api, domain}
import no.ndla.learningpathapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.mapping.License
import no.ndla.mapping.License.getLicenses
import no.ndla.network.model.CombinedUser
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.{reset, times, verify, when}
import sttp.client3.quick.*

import scala.util.{Failure, Success}

class LearningpathControllerV2Test extends UnitSuite with TestEnvironment with TapirControllerTest {
  val controller: LearningpathControllerV2 = new LearningpathControllerV2

  override def beforeEach(): Unit = {
    resetMocks()
    when(clock.now()).thenCallRealMethod()
    when(searchConverterService.asApiSearchResult(any)).thenCallRealMethod()
  }

  val copyright: api.CopyrightDTO =
    api.CopyrightDTO(commonApi.LicenseDTO(License.CC_BY_SA.toString, None, None), List())

  val DefaultLearningPathSummary: LearningPathSummaryV2DTO = api.LearningPathSummaryV2DTO(
    1,
    None,
    api.TitleDTO("Tittel", "nb"),
    api.DescriptionDTO("", "nb"),
    api.IntroductionDTO("", "nb"),
    "",
    None,
    None,
    "",
    NDLADate.now(),
    NDLADate.now(),
    api.LearningPathTagsDTO(Seq(), "nb"),
    copyright,
    List("nb"),
    None,
    None
  )

  test("That GET / will send all query-params to the search service") {
    val query              = "hoppetau"
    val tag                = "lek"
    val language           = "nb"
    val page               = 22
    val pageSize           = 111
    val ids                = "1,2"
    val verificationStatus = "EXTERNAL"

    val result    = SearchResult(1, Some(1), 1, language, Seq(DefaultLearningPathSummary), None)
    val apiResult = SearchResultV2DTO(1, Some(1), 1, language, Seq(DefaultLearningPathSummary))
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

    val queryParams = Map(
      "query"              -> query,
      "tag"                -> tag,
      "language"           -> language,
      "sort"               -> "-duration",
      "page-size"          -> s"$pageSize",
      "page"               -> s"$page",
      "ids"                -> s"$ids",
      "verificationStatus" -> s"$verificationStatus"
    )

    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths?$queryParams")
    )
    res.code.code should be(200)
    val convertedBody = CirceUtil.unsafeParseAs[api.SearchResultV2DTO](res.body)
    convertedBody.results.head.title should equal(api.TitleDTO("Tittel", "nb"))
  }

  test("That GET / will handle all empty query-params as missing query params") {
    val query    = ""
    val tag      = ""
    val language = ""
    val duration = ""
    val ids      = "1,2"

    val result    = SearchResult(-1, Some(1), 1, "nb", Seq(DefaultLearningPathSummary), None)
    val apiResult = SearchResultV2DTO(-1, Some(1), 1, "nb", Seq(DefaultLearningPathSummary))
    when(searchConverterService.asApiSearchResult(result)).thenReturn(apiResult)

    when(searchService.matchingQuery(any[SearchSettings])).thenReturn(Success(result))

    val queryParams = Map(
      "query"    -> query,
      "tag"      -> tag,
      "language" -> language,
      "sort"     -> duration,
      "ids"      -> s"$ids"
    )
    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths?$queryParams")
    )
    res.code.code should be(200)
    val convertedBody = CirceUtil.unsafeParseAs[api.SearchResultV2DTO](res.body)
    convertedBody.totalCount should be(-1)

  }

  test("That POST /search will send all query-params to the search service") {
    val query    = "hoppetau"
    val tag      = "lek"
    val language = "nb"
    val page     = 22
    val pageSize = 111

    val result    = SearchResult(1, Some(page), pageSize, language, Seq(DefaultLearningPathSummary), None)
    val apiResult = SearchResultV2DTO(1, Some(page), pageSize, language, Seq(DefaultLearningPathSummary))
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
    val inputBody =
      s"""{"query": "$query", "tag": "$tag", "language": "$language", "page": $page, "pageSize": $pageSize, "ids": [1, 2], "sort": "-duration" }"""
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/search/")
        .body(inputBody)
    )
    res.code.code should be(200)
    val convertedBody = CirceUtil.unsafeParseAs[api.SearchResultV2DTO](res.body)
    convertedBody.results.head.title should equal(api.TitleDTO("Tittel", "nb"))
  }

  test("That GET /licenses with filter sat to by only returns creative common licenses") {
    val creativeCommonlicenses = getLicenses
      .filter(_.license.toString.startsWith("by"))
      .map(l => commonApi.LicenseDTO(l.license.toString, Option(l.description), l.url))
      .toSet
    val res = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/licenses/?filter=by")
    )
    res.code.code should be(200)
    val convertedBody = CirceUtil.unsafeParseAs[Set[commonApi.LicenseDTO]](res.body)
    convertedBody should equal(creativeCommonlicenses)
  }

  test("That GET /licenses with filter not specified returns all licenses") {
    val allLicenses = getLicenses
      .map(l => commonApi.LicenseDTO(l.license.toString, Option(l.description), l.url))
      .toSet

    val res = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/licenses/")
    )
    res.code.code should be(200)
    val convertedBody = CirceUtil.unsafeParseAs[Set[commonApi.LicenseDTO]](res.body)
    convertedBody should equal(allLicenses)
  }

  test("That /with-status returns 400 if invalid status is specified") {
    when(readService.learningPathWithStatus(any[String], any[CombinedUser]))
      .thenReturn(Failure(InvalidLpStatusException("Bad status")))

    val res = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/status/invalidStatusHurrDurr")
    )
    res.code.code should be(400)

    when(readService.learningPathWithStatus(any[String], any[CombinedUser]))
      .thenReturn(Success(List.empty))

    val res2 = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/status/unlisted")
    )
    res2.code.code should be(200)
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

    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/")
    )
    res.code.code should be(200)
    res.body.contains(scrollId) should be(false)
    res.header("search-context") should be(Some(scrollId))
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

    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/?search-context=$scrollId")
    )
    res.code.code should be(200)

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

    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/search/")
        .body(s"""{"scrollId":"$scrollId"}""")
    )
    res.code.code should be(200)

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

    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/?search-context=initial")
    )
    res.code.code should be(200)
    verify(searchService, times(1)).matchingQuery(expectedSettings)
    verify(searchService, times(0)).scroll(any[String], any[String])
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
    when(searchService.containsArticle(any)).thenReturn(Success(result.results))

    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/contains-article/123")
    )
    res.code.code should be(200)
  }

  test("That GET /contains-article returns correct errors when id is a string or nothing") {
    reset(taxonomyApiClient)
    when(taxonomyApiClient.queryNodes(any[Long])).thenReturn(Success(List[Node]()))

    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/contains-article/hallohallo")
    )
    res.code.code should be(400)

    val res2 = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/learningpath-api/v2/learningpaths/contains-article/")
    )
    res2.code.code should be(400)
  }
}
