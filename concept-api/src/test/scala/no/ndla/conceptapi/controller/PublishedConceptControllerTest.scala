/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package no.ndla.conceptapi.controller

import no.ndla.conceptapi.model.api.{ConceptSummary, NotFoundException}
import no.ndla.conceptapi.model.domain.{SearchResult, Sort}
import no.ndla.conceptapi.model.search.SearchSettings
import no.ndla.conceptapi.{ConceptSwagger, TestData, TestEnvironment, UnitSuite}
import org.json4s.DefaultFormats
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.{Failure, Success}

class PublishedConceptControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  implicit val swagger = new ConceptSwagger
  lazy val controller = new PublishedConceptController
  addServlet(controller, "/test")

  val conceptId = 1
  val lang = "nb"

  val invalidConcept = """{"title": [{"language": "nb", "titlee": "lol"]}"""

  override def beforeEach(): Unit = {
    when(user.getUser).thenReturn(TestData.userWithWriteAccess)
  }

  test("/<concept_id> should return 200 if the concept was found") {
    when(readService.publishedConceptWithId(conceptId, lang, fallback = false))
      .thenReturn(Success(TestData.sampleNbApiConcept))

    get(s"/test/$conceptId?language=$lang") {
      status should equal(200)
    }
  }

  test("/<concept_id> should return 404 if the concept was not found") {
    when(readService.publishedConceptWithId(conceptId, lang, fallback = false))
      .thenReturn(Failure(NotFoundException("Not found, yolo")))

    get(s"/test/$conceptId?language=$lang") {
      status should equal(404)
    }
  }

  test("/<concept_id> should return 400 if the id was not valid") {
    get(s"/test/one") {
      status should equal(400)
    }
  }

  test("GET /tags should return 200 on getting all tags") {
    when(readService.allTagsFromConcepts(lang, fallback = false))
      .thenReturn(List("tag1", "tag2"))

    get(s"/test/tags/?language=$lang") {
      status should equal(200)
    }
  }

  test("that scrolling published doesn'thappen on 'initial'") {
    reset(publishedConceptSearchService)

    val multiResult =
      SearchResult[ConceptSummary](0, None, 10, "nn", Seq.empty, Some("heiheihei"))
    when(publishedConceptSearchService.all(any[SearchSettings])).thenReturn(Success(multiResult))

    val expectedSettings =
      SearchSettings.empty.copy(shouldScroll = true, pageSize = 10, sort = Sort.ByTitleDesc)

    get("/test/?search-context=initial") {
      status should be(200)
      verify(publishedConceptSearchService, times(1)).all(eqTo(expectedSettings))
    }
  }

}
