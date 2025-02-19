/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Availability
import no.ndla.network.clients.FeideExtendedUserInfo
import no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.domain.Sort
import no.ndla.searchapi.model.search.settings.{MultiDraftSearchSettings, SearchSettings}
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, times, verify, when}
import sttp.client3.quick.*

import java.time.Month
import scala.util.Success

class SearchControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override val converterService    = new ConverterService
  val controller: SearchController = new SearchController()

  override def beforeEach(): Unit = {
    reset(clock)
    reset(searchConverterService)
    when(searchConverterService.toApiMultiSearchResult(any)).thenCallRealMethod()
    when(clock.now()).thenCallRealMethod()
  }

  val authTokenWithNoRole =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6W10sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.vw9YhRtgUQr_vuDhLNHfBsZz-4XLhCc1Kwxi0w0_qGI"
  val authTokenWithWriteRole =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJkcmFmdHM6d3JpdGUiXSwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.5jpF98NxQZlkQQ5-rxVO3oTkNOQRQLDlAexyDnLiZFY"

  val authHeadersWithWriteRole: Map[String, String] = Map("Authorization" -> s"Bearer $authTokenWithWriteRole")
  val authHeadersWithNoRole: Map[String, String]    = Map("Authorization" -> s"Bearer $authTokenWithNoRole")

  test("That / returns 200 ok") {
    val multiResult =
      domain.SearchResult(0, Some(1), 10, "nb", Seq.empty, suggestions = Seq.empty, aggregations = Seq.empty)
    when(multiSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(multiResult))
    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/search-api/v1/search/")
      )
      .code
      .code should be(200)
  }

  test("That /group/ returns 200 ok") {
    val multiResult =
      domain.SearchResult(0, Some(1), 10, "nb", Seq.empty, suggestions = Seq.empty, aggregations = Seq.empty)
    when(multiSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(multiResult))
    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/search-api/v1/search/?resource-types=test")
      )
      .code
      .code should be(200)
  }

  test("That / returns scrollId if not scrolling, but scrollId is returned") {
    reset(multiSearchService)
    val validScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nb", Seq.empty, Seq.empty, Seq.empty, Some(validScrollId))

    when(multiSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(multiResult))
    val response = simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/search-api/v1/search/")
      )
    response.code.code should be(200)
    response.headers("search-context").head should be(validScrollId)
    verify(multiSearchService, times(1)).matchingQuery(any[SearchSettings])
  }

  test("That /editorial/ returns scrollId if not scrolling, but scrollId is returned") {
    reset(multiDraftSearchService)
    val validScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nb", Seq.empty, Seq.empty, Seq.empty, Some(validScrollId))

    when(multiDraftSearchService.matchingQuery(any[MultiDraftSearchSettings])).thenReturn(Success(multiResult))
    val response = simpleHttpClient
      .send(
        quickRequest
          .post(uri"http://localhost:$serverPort/search-api/v1/search/editorial/")
          .headers(authHeadersWithWriteRole)
      )
    response.code.code should be(200)
    response.headers("search-context").head should be(validScrollId)
    verify(multiDraftSearchService, times(1)).matchingQuery(any[MultiDraftSearchSettings])
  }

  test("That / scrolls if scrollId is specified") {
    reset(multiSearchService)
    val validScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val newValidScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAtAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Seq.empty, Some(newValidScrollId))

    when(multiSearchService.scroll(eqTo(validScrollId), eqTo("nn"))).thenReturn(Success(multiResult))
    val response = simpleHttpClient.send(
      quickRequest
        .get(
          uri"http://localhost:$serverPort/search-api/v1/search/?search-context=$validScrollId&language=nn&fallback=true"
        )
    )
    response.code.code should be(200)
    response.headers("search-context").head should be(newValidScrollId)
    verify(multiSearchService, times(1)).scroll(eqTo(validScrollId), eqTo("nn"))
  }

  test("That /editorial/ scrolls if scrollId is specified") {
    reset(multiDraftSearchService)
    val validScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val newValidScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAtAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Seq.empty, Some(newValidScrollId))

    when(multiDraftSearchService.scroll(eqTo(validScrollId), eqTo("nn"))).thenReturn(Success(multiResult))
    val response = simpleHttpClient.send(
      quickRequest
        .post(
          uri"http://localhost:$serverPort/search-api/v1/search/editorial/"
        )
        .body(s"""{"scrollId":"$validScrollId","language":"nn","fallback":true}""")
        .headers(authHeadersWithWriteRole)
    )
    response.code.code should equal(200)
    response.headers("search-context").head should be(newValidScrollId)
    verify(multiDraftSearchService, times(1)).scroll(eqTo(validScrollId), eqTo("nn"))
  }

  test("That /editorial/ returns access denied if user does not have drafts:write role") {
    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/search-api/v1/search/editorial/")
        .headers(authHeadersWithNoRole)
    )
    response.code.code should equal(403)
  }

  test("That draft scrolling doesn't happen on 'initial' scrollId") {
    reset(multiDraftSearchService, multiSearchService)
    val newValidScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAtAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Seq.empty, Some(newValidScrollId))
    when(multiDraftSearchService.matchingQuery(any[MultiDraftSearchSettings])).thenReturn(Success(multiResult))

    val response = simpleHttpClient.send(
      quickRequest
        .post(
          uri"http://localhost:$serverPort/search-api/v1/search/editorial/"
        )
        .body("""{"scrollId":"initial","language":"nn","fallback":true}""")
        .headers(authHeadersWithWriteRole)
    )
    response.code.code should be(200)
    response.headers("search-context").head should be(newValidScrollId)

    val expectedSettings =
      TestData.multiDraftSearchSettings.copy(
        fallback = true,
        language = "nn",
        pageSize = 10,
        shouldScroll = true,
        sort = Sort.ByRelevanceDesc
      )

    verify(multiDraftSearchService, times(0)).scroll(any[String], any[String])
    verify(multiDraftSearchService, times(1)).matchingQuery(eqTo(expectedSettings))
    verify(multiSearchService, times(0)).scroll(any[String], any[String])
    verify(multiSearchService, times(0)).matchingQuery(any[SearchSettings])

  }

  test("That scrolling doesn't happen on 'initial' scrollId") {
    reset(multiDraftSearchService, multiSearchService)
    val newValidScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAtAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Seq.empty, Some(newValidScrollId))
    when(multiSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(multiResult))

    val response = simpleHttpClient.send(
      quickRequest
        .get(
          uri"http://localhost:$serverPort/search-api/v1/search/?search-context=initial&language=nn&fallback=true"
        )
        .headers(authHeadersWithWriteRole)
    )
    response.code.code should be(200)
    response.headers("search-context").head should be(newValidScrollId)

    val expectedSettings =
      TestData.searchSettings.copy(
        fallback = true,
        language = "nn",
        pageSize = 10,
        shouldScroll = true,
        sort = Sort.ByRelevanceDesc,
        resultTypes = Some(List.empty)
      )

    verify(multiDraftSearchService, times(0)).scroll(any[String], any[String])
    verify(multiDraftSearchService, times(0)).matchingQuery(any[MultiDraftSearchSettings])
    verify(multiSearchService, times(0)).scroll(any[String], any[String])
    verify(multiSearchService, times(1)).matchingQuery(expectedSettings)

  }

  test("That fetching feide user doesnt happen if no token is supplied") {
    reset(multiSearchService)
    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Seq.empty, None)
    when(multiSearchService.matchingQuery(any)).thenReturn(Success(multiResult))

    val baseSettings = TestData.searchSettings.copy(
      language = "*",
      pageSize = 10,
      sort = Sort.ByRelevanceDesc,
      resultTypes = Some(List.empty)
    )

    val response = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/search-api/v1/search/")
    )

    val expectedSettings = baseSettings.copy(availability = List())
    response.code.code should be(200)
    verify(multiSearchService, times(1)).matchingQuery(eqTo(expectedSettings))
    verify(feideApiClient, never).getFeideExtendedUser(any)
  }

  test("That fetching feide user does happen if token is supplied") {
    reset(multiSearchService)
    val teacheruser = FeideExtendedUserInfo(
      displayName = "Johnny Bravo",
      eduPersonAffiliation = Seq("employee", "staff"),
      eduPersonPrimaryAffiliation = None,
      eduPersonPrincipalName = "example@email.com",
      mail = Some(Seq("example@email.com"))
    )
    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Seq.empty, None)
    when(feideApiClient.getFeideExtendedUser(any)).thenReturn(Success(teacheruser))
    when(multiSearchService.matchingQuery(any)).thenReturn(Success(multiResult))

    val baseSettings = TestData.searchSettings.copy(
      language = "*",
      pageSize = 10,
      sort = Sort.ByRelevanceDesc,
      resultTypes = Some(List.empty)
    )
    val teacherToken = "abcd"

    val response = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/search-api/v1/search/")
        .header("FeideAuthorization", teacherToken)
    )
    val expectedSettings = baseSettings.copy(
      availability = List(
        Availability.everyone,
        Availability.teacher
      )
    )
    response.code.code should be(200)
    verify(multiSearchService, times(1)).matchingQuery(eqTo(expectedSettings))
    verify(feideApiClient, times(1)).getFeideExtendedUser(eqTo(Some(teacherToken)))
  }

  test("That retrieving datetime strings from request works") {
    reset(multiDraftSearchService)
    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Seq.empty, None)
    when(multiDraftSearchService.matchingQuery(any)).thenReturn(Success(multiResult))

    val response = simpleHttpClient.send(
      quickRequest
        .post(
          uri"http://localhost:$serverPort/search-api/v1/search/editorial/"
        )
        .body("""{"revisionDateFrom":"2025-01-02T13:39:05Z","revisionDateTo":"2025-01-02T13:39:05Z"}""")
        .headers(authHeadersWithWriteRole)
    )

    response.code.code should be(200)
    val expectedDate = NDLADate.of(2025, Month.JANUARY, 2, 13, 39, 5)

    val captor: ArgumentCaptor[MultiDraftSearchSettings] = ArgumentCaptor.forClass(classOf[MultiDraftSearchSettings])
    verify(multiDraftSearchService, times(1)).matchingQuery(captor.capture())

    captor.getValue.revisionDateFilterFrom should be(Some(expectedDate))
    captor.getValue.revisionDateFilterTo should be(Some(expectedDate))

  }

}
