/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package no.ndla.conceptapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.conceptapi.model.api.{ConceptSummary, NotFoundException}
import no.ndla.conceptapi.model.domain.{SearchResult, Sort}
import no.ndla.conceptapi.model.search.SearchSettings
import no.ndla.conceptapi.{Eff, TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.Service
import org.json4s.DefaultFormats
import sttp.client3.quick._

import scala.util.{Failure, Success}

class PublishedConceptControllerTest extends UnitSuite with TestEnvironment {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val serverPort: Int                       = findFreePort
  val controller                            = new PublishedConceptController
  override val services: List[Service[Eff]] = List(controller)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer(this.getClass.getName, serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  override def beforeEach(): Unit = {
    reset(clock, searchConverterService)
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
      SearchResult[ConceptSummary](0, None, 10, "nn", Seq.empty, Seq.empty, Some("heiheihei"))
    when(publishedConceptSearchService.all(any[SearchSettings])).thenReturn(Success(multiResult))
    when(searchConverterService.asApiConceptSearchResult(any)).thenCallRealMethod()

    val expectedSettings =
      SearchSettings.empty.copy(shouldScroll = true, pageSize = 10, sort = Sort.ByTitleDesc)

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/concept-api/v1/concepts/?search-context=initial")
      )
      .code
      .code should be(200)
    verify(publishedConceptSearchService, times(1)).all(eqTo(expectedSettings))
  }

  test("That no endpoints are shadowed") {
    import sttp.tapir.testing.EndpointVerifier
    val errors = EndpointVerifier(controller.endpoints.map(_.endpoint))
    if (errors.nonEmpty) {
      val errString = errors.map(e => e.toString).mkString("\n\t- ", "\n\t- ", "")
      fail(s"Got errors when verifying ${controller.serviceName} controller:$errString")
    }
  }

}
