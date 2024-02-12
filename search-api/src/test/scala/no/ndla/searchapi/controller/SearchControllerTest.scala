/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Availability
import no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.domain.{SearchParams, Sort}
import no.ndla.searchapi.model.search.settings.{MultiDraftSearchSettings, SearchSettings}
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.clients.FeideExtendedUserInfo
import org.mockito.Strictness
import org.scalatra.test.scalatest.ScalatraFunSuite

import java.time.Month
import javax.servlet.http.HttpServletRequest
import scala.util.Success

class SearchControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  val swagger         = new SearchSwagger
  lazy val controller = new SearchController()(swagger)
  addServlet(controller, "/test")

  val authTokenWithNoRole =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6W10sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.vw9YhRtgUQr_vuDhLNHfBsZz-4XLhCc1Kwxi0w0_qGI"
  val authTokenWithWriteRole =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJkcmFmdHM6d3JpdGUiXSwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.5jpF98NxQZlkQQ5-rxVO3oTkNOQRQLDlAexyDnLiZFY"

  val authHeadersWithWriteRole: Map[String, String] = Map("Authorization" -> s"Bearer $authTokenWithWriteRole")
  val authHeadersWithNoRole: Map[String, String]    = Map("Authorization" -> s"Bearer $authTokenWithNoRole")

  test("That /draft/ returns 200 ok") {
    when(searchService.search(any[SearchParams], any[Set[SearchApiClient]])).thenReturn(Seq.empty)
    get("/test/draft/") {
      status should equal(200)
    }
  }

  test("That / returns 200 ok") {
    val multiResult =
      domain.SearchResult(0, Some(1), 10, "nb", Seq.empty, suggestions = Seq.empty, aggregations = Seq.empty)
    when(multiSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(multiResult))
    get("/test/") {
      status should equal(200)
    }
  }

  test("That /group/ returns 200 ok") {
    val multiResult =
      domain.SearchResult(0, Some(1), 10, "nb", Seq.empty, suggestions = Seq.empty, aggregations = Seq.empty)
    when(multiSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(multiResult))
    get("/test/group/?resource-types=test") {
      status should equal(200)
    }
  }

  test("That / returns scrollId if not scrolling, but scrollId is returned") {
    reset(multiSearchService)
    val validScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nb", Seq.empty, Seq.empty, Seq.empty, Some(validScrollId))

    when(multiSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(multiResult))
    get(s"/test/") {
      status should equal(200)
      response.headers("search-context").head should be(validScrollId)
    }

    verify(multiSearchService, times(1)).matchingQuery(any[SearchSettings])
  }

  test("That /editorial/ returns scrollId if not scrolling, but scrollId is returned") {
    reset(multiDraftSearchService)
    val validScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nb", Seq.empty, Seq.empty, Seq.empty, Some(validScrollId))

    when(multiDraftSearchService.matchingQuery(any[MultiDraftSearchSettings])).thenReturn(Success(multiResult))
    get(s"/test/editorial/", headers = authHeadersWithWriteRole) {
      status should equal(200)
      response.headers("search-context").head should be(validScrollId)
    }

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
    get(s"/test/?search-context=$validScrollId&language=nn&fallback=true") {
      status should equal(200)
      response.headers("search-context").head should be(newValidScrollId)
    }

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
    get(
      s"/test/editorial/?search-context=$validScrollId&language=nn&fallback=true",
      headers = authHeadersWithWriteRole
    ) {
      status should equal(200)
      response.headers("search-context").head should be(newValidScrollId)
    }

    verify(multiDraftSearchService, times(1)).scroll(eqTo(validScrollId), eqTo("nn"))
  }

  test("That /editorial/ returns access denied if user does not have drafts:write role") {
    get(s"/test/editorial/", headers = authHeadersWithNoRole) {
      status should equal(403)
    }
  }

  test("That draft scrolling doesn't happen on 'initial' scrollId") {
    reset(multiDraftSearchService, multiSearchService)
    val newValidScrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAtAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="

    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Seq.empty, Some(newValidScrollId))
    when(multiDraftSearchService.matchingQuery(any[MultiDraftSearchSettings])).thenReturn(Success(multiResult))

    get(s"/test/editorial/?search-context=initial&language=nn&fallback=true", headers = authHeadersWithWriteRole) {
      status should equal(200)
      response.headers("search-context").head should be(newValidScrollId)
    }

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

    get(s"/test/?search-context=initial&language=nn&fallback=true", headers = authHeadersWithWriteRole) {
      status should equal(200)
      response.headers("search-context").head should be(newValidScrollId)
    }

    val expectedSettings =
      TestData.searchSettings.copy(
        fallback = true,
        language = "nn",
        pageSize = 10,
        shouldScroll = true,
        sort = Sort.ByRelevanceDesc
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

    val baseSettings = TestData.searchSettings.copy(language = "*", pageSize = 10, sort = Sort.ByRelevanceDesc)

    get("/test/", params = Seq.empty, headers = Seq()) {
      val expectedSettings = baseSettings.copy(availability = List())
      status should be(200)
      verify(multiSearchService, times(1)).matchingQuery(eqTo(expectedSettings))
    }

    verify(feideApiClient, never).getFeideExtendedUser(any)
  }

  test("That fetching feide user does happen if token is supplied") {
    reset(multiSearchService)
    val teacheruser = FeideExtendedUserInfo(
      displayName = "Johnny Bravo",
      eduPersonAffiliation = Seq("employee", "staff"),
      eduPersonPrincipalName = "example@email.com",
      mail = Seq("example@email.com")
    )
    val multiResult = domain.SearchResult(0, None, 10, "nn", Seq.empty, Seq.empty, Seq.empty, None)
    when(feideApiClient.getFeideExtendedUser(any)).thenReturn(Success(teacheruser))
    when(multiSearchService.matchingQuery(any)).thenReturn(Success(multiResult))

    val baseSettings = TestData.searchSettings.copy(language = "*", pageSize = 10, sort = Sort.ByRelevanceDesc)
    val teacherToken = "abcd"

    get("/test/", params = Seq.empty, headers = Seq("FeideAuthorization" -> teacherToken)) {
      val expectedSettings = baseSettings.copy(
        availability = List(
          Availability.everyone,
          Availability.teacher
        )
      )
      status should be(200)
      verify(multiSearchService, times(1)).matchingQuery(eqTo(expectedSettings))
    }

    verify(feideApiClient, times(1)).getFeideExtendedUser(eqTo(Some(teacherToken)))
  }

  test("That retrieving datetime strings from request works") {
    val requestMock = mock[HttpServletRequest](withSettings.strictness(Strictness.Lenient))

    val expectedDate = NDLADate.of(2025, Month.JANUARY, 2, 13, 39, 5)

    when(requestMock.getQueryString).thenReturn(
      "revision-date-from=2025-01-02T13:39:05Z&revision-date-to=2025-01-02T13:39:05Z"
    )
    val settings = controller.getDraftSearchSettingsFromRequest(requestMock)
    settings.revisionDateFilterFrom should be(Some(expectedDate))
    settings.revisionDateFilterTo should be(Some(expectedDate))
  }

}
