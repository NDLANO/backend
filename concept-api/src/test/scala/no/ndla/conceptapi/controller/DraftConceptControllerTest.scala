/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package no.ndla.conceptapi.controller

import no.ndla.conceptapi.model.api.{ConceptSummary, NewConcept, NewConceptMetaImage, NotFoundException, UpdatedConcept}
import no.ndla.conceptapi.{ConceptSwagger, TestData, TestEnvironment}
import no.ndla.conceptapi.UnitSuite
import no.ndla.conceptapi.auth.UserInfo
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.domain.{SearchResult, Sort}
import no.ndla.conceptapi.model.search.{DraftSearchSettings, SearchSettings}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write
import org.mockito.ArgumentMatchers.{any, eq => eqTo, _}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.{Failure, Success}

class DraftConceptControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  implicit val swagger = new ConceptSwagger
  lazy val controller = new DraftConceptController
  addServlet(controller, "/test")

  val conceptId = 1
  val lang = "nb"

  val invalidConcept = """{"title": [{"language": "nb", "titlee": "lol"]}"""

  override def beforeEach(): Unit = {
    when(user.getUser).thenReturn(TestData.userWithWriteAccess)
  }

  test("/<concept_id> should return 200 if the concept was found") {
    when(readService.conceptWithId(conceptId, lang, fallback = false))
      .thenReturn(Success(TestData.sampleNbApiConcept))

    get(s"/test/$conceptId?language=$lang") {
      status should equal(200)
    }
  }

  test("/<concept_id> should return 404 if the concept was not found") {
    when(readService.conceptWithId(conceptId, lang, fallback = false))
      .thenReturn(Failure(NotFoundException("Not found, yolo")))

    get(s"/test/$conceptId?language=$lang") {
      status should equal(404)
    }
  }

  test("/<concept_id> should return 400 if the concept was not found") {
    get(s"/test/one") {
      status should equal(400)
    }
  }

  test("POST / should return 400 if body does not contain all required fields") {
    post("/test/", invalidConcept) {
      status should equal(400)
    }
  }

  test("POST / should return 201 on created") {
    when(
      writeService
        .newConcept(any[NewConcept], any[UserInfo]))
      .thenReturn(Success(TestData.sampleNbApiConcept))
    post("/test/", write(TestData.sampleNewConcept), headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(201)
    }
  }

  test("POST / should return 403 if no write role") {
    when(user.getUser).thenReturn(TestData.userWithNoRoles)
    when(
      writeService
        .newConcept(any[NewConcept], any[UserInfo]))
      .thenReturn(Success(TestData.sampleNbApiConcept))
    post("/test/", write(TestData.sampleNewConcept), headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(403)
    }
  }

  test("PATCH / should return 200 on updated") {
    when(
      writeService
        .updateConcept(eqTo(1.toLong), any[UpdatedConcept], any[UserInfo]))
      .thenReturn(Success(TestData.sampleNbApiConcept))

    patch("/test/1", write(TestData.updatedConcept), headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(200)
    }
  }

  test("PATCH / should return 403 if no write role") {
    when(user.getUser).thenReturn(TestData.userWithNoRoles)
    when(
      writeService
        .updateConcept(eqTo(1.toLong), any[UpdatedConcept], any[UserInfo]))
      .thenReturn(Success(TestData.sampleNbApiConcept))

    patch("/test/1", write(TestData.updatedConcept), headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(403)
    }
  }

  test("PATCH / should return 200 on updated, checking json4s deserializer of Either[Null, Option[Long]]") {
    reset(writeService)
    when(
      writeService
        .updateConcept(eqTo(1.toLong), any[UpdatedConcept], any[UserInfo]))
      .thenReturn(Success(TestData.sampleNbApiConcept))

    val missing = """{"language":"nb"}"""
    val missingExpected = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Right(None))

    val nullArtId = """{"language":"nb","metaImage":null}"""
    val nullExpected = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Left(null))

    val existingArtId = """{"language":"nb","metaImage":{"id":"123","alt":"alt123"}}"""
    val existingExpected = TestData.emptyApiUpdatedConcept
      .copy(language = "nb", metaImage = Right(Some(NewConceptMetaImage(id = "123", alt = "alt123"))))

    patch("/test/1", missing, headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(missingExpected), any[UserInfo])
    }

    patch("/test/1", nullArtId, headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(nullExpected), any[UserInfo])
    }

    patch("/test/1", existingArtId, headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(existingExpected), any[UserInfo])
    }
  }

  test("tags should return 200 OK if the result was not empty") {
    when(readService.getAllTags(anyString, anyInt, anyInt, anyString))
      .thenReturn(TestData.sampleApiTagsSearchResult)

    get("/test/tag-search/") {
      status should equal(200)
    }
  }

  test(
    "PATCH / should return 200 on updated, checking json4s deserializer of Either[Null, Option[NewConceptMetaImage]]") {
    reset(writeService)
    when(
      writeService
        .updateConcept(eqTo(1.toLong), any[UpdatedConcept], any[UserInfo]))
      .thenReturn(Success(TestData.sampleNbApiConcept))

    val missing = """{"language":"nb"}"""
    val missingExpected = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Right(None))

    val nullArtId = """{"language":"nb","metaImage":null}"""
    val nullExpected = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Left(null))

    val existingArtId = """{"language":"nb","metaImage": {"id": "1",
                          |		"alt": "alt-text"}}""".stripMargin
    val existingExpected = TestData.emptyApiUpdatedConcept
      .copy(language = "nb", metaImage = Right(Some(api.NewConceptMetaImage("1", "alt-text"))))

    patch("/test/1", missing, headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(missingExpected), any[UserInfo])
    }

    patch("/test/1", nullArtId, headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(nullExpected), any[UserInfo])
    }

    patch("/test/1", existingArtId, headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should equal(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(existingExpected), any[UserInfo])
    }
  }

  test("that scrolling doesn't happen on 'initial'") {
    reset(draftConceptSearchService)

    val multiResult =
      SearchResult[ConceptSummary](0, None, 10, "nn", Seq.empty, Some("heiheihei"))
    when(draftConceptSearchService.all(any[DraftSearchSettings])).thenReturn(Success(multiResult))

    val expectedSettings =
      DraftSearchSettings.empty.copy(shouldScroll = true, pageSize = 10, sort = Sort.ByTitleDesc)

    get("/test/?search-context=initial") {
      status should be(200)
      verify(draftConceptSearchService, times(1)).all(eqTo(expectedSettings))
    }
  }

}
