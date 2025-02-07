/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.conceptapi.controller

import no.ndla.conceptapi.model.api.{ConceptSummaryDTO, NotFoundException}
import no.ndla.conceptapi.model.domain.{SearchResult, Sort}
import no.ndla.conceptapi.model.search.SearchSettings
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import sttp.client3.quick.*

import scala.util.{Failure, Success}

class PublishedConceptControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  val controller: PublishedConceptController = new PublishedConceptController

  override def beforeEach(): Unit = {
    reset(clock)
    reset(searchConverterService)
    when(clock.now()).thenCallRealMethod()
  }

  val conceptId = 1L
  val lang      = "nb"

  val invalidConcept = """{"title": [{"language": "nb", "titlee": "lol"]}"""

  test("/<concept_id> should return 200 if the concept was found") {
    when(readService.publishedConceptWithId(conceptId, lang, fallback = false, None))
      .thenReturn(Success(TestData.sampleNbApiConcept))

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/concept-api/v1/concepts/$conceptId?language=$lang")
      )
      .code
      .code should be(200)
  }

  test("/<concept_id> should return 404 if the concept was not found") {
    when(readService.publishedConceptWithId(conceptId, lang, fallback = false, None))
      .thenReturn(Failure(NotFoundException("Not found, yolo")))

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/concept-api/v1/concepts/$conceptId?language=$lang")
      )
      .code
      .code should be(404)
  }

  test("/<concept_id> should return 400 if the id was not valid") {
    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/concept-api/v1/concepts/one")
      )
      .code
      .code should be(400)
  }

  test("GET /tags should return 200 on getting all tags") {
    when(readService.allTagsFromConcepts(lang, fallback = false))
      .thenReturn(List("tag1", "tag2"))

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/concept-api/v1/concepts/tags/?language=$lang")
      )
      .code
      .code should be(200)
  }

  test("that scrolling published doesn't happen on 'initial'") {
    reset(publishedConceptSearchService)

    val multiResult =
      SearchResult[ConceptSummaryDTO](0, None, 10, "nn", Seq.empty, Seq.empty, Some("heiheihei"))
    when(publishedConceptSearchService.all(any[SearchSettings])).thenReturn(Success(multiResult))
    when(searchConverterService.asApiConceptSearchResult(any)).thenCallRealMethod()

    val expectedSettings =
      SearchSettings.empty.copy(pageSize = 10, sort = Sort.ByTitleDesc, shouldScroll = true)

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/concept-api/v1/concepts/?search-context=initial")
      )
      .code
      .code should be(200)
    verify(publishedConceptSearchService, times(1)).all(eqTo(expectedSettings))
  }
}
