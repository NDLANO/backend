/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.model.domain.{AgreementSearchSettings, SearchResult, Sort}
import no.ndla.draftapi.model.api
import no.ndla.draftapi.{DraftSwagger, TestData, TestEnvironment, UnitSuite}
import org.json4s.DefaultFormats
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.Success

class AgreementControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  implicit val swagger: DraftSwagger = new DraftSwagger

  lazy val controller = new AgreementController
  addServlet(controller, "/test")

  val agreementId = 1

  override def beforeEach(): Unit = {
    when(user.getUser).thenReturn(TestData.userWithWriteAccess)
  }

  test("/<agreement_id> should return 200 if the agreement was found") {
    when(readService.agreementWithId(1)).thenReturn(Some(TestData.sampleApiAgreement))
    get(s"/test/$agreementId") {
      status should equal(200)
    }
  }

  test("/<agreement_id> should return 404 if the article was not found") {
    when(readService.agreementWithId(agreementId)).thenReturn(None)

    get(s"/test/$agreementId") {
      status should equal(404)
    }
  }

  test("/<agreement_id> should return 400 if the request was bad") {
    get(s"/test/one") {
      status should equal(400)
    }
  }

  test("That scrollId is in header, and not in body") {
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult[api.AgreementSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[api.AgreementSummary],
      Some(scrollId)
    )
    when(agreementSearchService.matchingQuery(any[AgreementSearchSettings]))
      .thenReturn(Success(searchResponse))

    get(s"/test/") {
      status should be(200)
      body.contains(scrollId) should be(false)
      header("search-context") should be(scrollId)
    }
  }

  test("That scrolling uses scroll and not searches normally") {
    reset(agreementSearchService)
    val scrollId =
      "DnF1ZXJ5VGhlbkZldGNoCgAAAAAAAAC1Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAthYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALcWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC4Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuRYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAALsWLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC9Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFEAAAAAAAAAuhYtY2VPYWFvRFQ5aWNSbzRFYVZSTEhRAAAAAAAAAL4WLWNlT2Fhb0RUOWljUm80RWFWUkxIUQAAAAAAAAC8Fi1jZU9hYW9EVDlpY1JvNEVhVlJMSFE="
    val searchResponse = SearchResult[api.AgreementSummary](
      0,
      Some(1),
      10,
      "nb",
      Seq.empty[api.AgreementSummary],
      Some(scrollId)
    )

    when(agreementSearchService.scroll(anyString, anyString)).thenReturn(Success(searchResponse))

    get(s"/test?search-context=$scrollId") {
      status should be(200)
    }

    verify(agreementSearchService, times(0)).matchingQuery(any[AgreementSearchSettings])
    verify(agreementSearchService, times(1)).scroll(eqTo(scrollId), any[String])
  }

}
