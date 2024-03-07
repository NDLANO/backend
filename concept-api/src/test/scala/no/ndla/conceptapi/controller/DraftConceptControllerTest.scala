/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package no.ndla.conceptapi.controller

import no.ndla.common.model.api.{Delete, Missing, UpdateWith}
import no.ndla.conceptapi.model.api._
import no.ndla.conceptapi.model.domain.{SearchResult, Sort}
import no.ndla.conceptapi.model.{api, search}
import no.ndla.conceptapi.{Eff, TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.tapirtesting.TapirControllerTest
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.mockito.ArgumentMatchers._
import sttp.client3.quick._

import scala.util.{Failure, Success}

class DraftConceptControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {
  implicit val formats: Formats = org.json4s.DefaultFormats
  val controller                = new DraftConceptController

  override def beforeEach(): Unit = {
    reset(clock, searchConverterService)
    when(clock.now()).thenCallRealMethod()
  }

  val conceptId = 1L
  val lang      = "nb"

  val invalidConcept = """{"title": [{"language": "nb", "titlee": "lol"]}"""

  test("/<concept_id> should return 200 if the concept was found") {
    when(readService.conceptWithId(conceptId, lang, fallback = false, None))
      .thenReturn(Success(TestData.sampleNbApiConcept))

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/concept-api/v1/drafts/$conceptId?language=$lang")
      )
      .code
      .code should be(200)
  }

  test("/<concept_id> should return 404 if the concept was not found") {
    when(readService.conceptWithId(conceptId, lang, fallback = false, None))
      .thenReturn(Failure(NotFoundException("Not found, yolo")))

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/concept-api/v1/drafts/$conceptId?language=$lang")
      )
      .code
      .code should be(404)
  }

  test("/<concept_id> should return 400 if the concept was not found") {
    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/concept-api/v1/drafts/one")
      )
      .code
      .code should be(400)
  }

  test("POST / should return 400 if body does not contain all required fields") {
    simpleHttpClient
      .send(
        quickRequest
          .post(uri"http://localhost:$serverPort/concept-api/v1/drafts")
          .body(invalidConcept)
          .header("Authorization", TestData.authHeaderWithWriteRole)
      )
      .code
      .code should be(400)
  }

  test("POST / should return 201 on created") {
    when(
      writeService
        .newConcept(any[NewConcept], any[TokenUser])
    )
      .thenReturn(Success(TestData.sampleNbApiConcept))
    simpleHttpClient
      .send(
        quickRequest
          .post(uri"http://localhost:$serverPort/concept-api/v1/drafts/")
          .body(write(TestData.sampleNewConcept))
          .header("Authorization", TestData.authHeaderWithWriteRole)
      )
      .code
      .code should be(201)
  }

  test("POST / should return 403 if no write role") {
    when(
      writeService
        .newConcept(any[NewConcept], any)
    )
      .thenReturn(Success(TestData.sampleNbApiConcept))
    simpleHttpClient
      .send(
        quickRequest
          .post(uri"http://localhost:$serverPort/concept-api/v1/drafts/")
          .body(write(TestData.sampleNewConcept))
          .header("Authorization", TestData.authHeaderWithWrongRole)
      )
      .code
      .code should be(403)
  }

  test("PATCH / should return 200 on updated") {
    when(
      writeService
        .updateConcept(eqTo(1.toLong), any[UpdatedConcept], any)
    )
      .thenReturn(Success(TestData.sampleNbApiConcept))

    import io.circe.syntax._
    val body = TestData.updatedConcept.asJson.deepDropNullValues.noSpaces

    val res = simpleHttpClient
      .send(
        quickRequest
          .patch(uri"http://localhost:$serverPort/concept-api/v1/drafts/1")
          .body(body)
          .header("Authorization", TestData.authHeaderWithWriteRole)
      )
    res.code.code should be(200)
  }

  test("PATCH / should return 403 if no write role") {
    when(
      writeService
        .updateConcept(eqTo(1.toLong), any[UpdatedConcept], any)
    )
      .thenReturn(Success(TestData.sampleNbApiConcept))

    simpleHttpClient
      .send(
        quickRequest
          .patch(uri"http://localhost:$serverPort/concept-api/v1/drafts/1")
          .body(write(TestData.updatedConcept))
          .header("Authorization", TestData.authHeaderWithoutAnyRoles)
      )
      .code
      .code should be(403)
  }

  test("PATCH / should return 200 on updated, checking json4s deserializer of Either[Null, Option[Long]]") {
    reset(writeService)
    when(
      writeService
        .updateConcept(eqTo(1.toLong), any[UpdatedConcept], any[TokenUser])
    )
      .thenReturn(Success(TestData.sampleNbApiConcept))

    val missing         = """{"language":"nb"}"""
    val missingExpected = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Missing)

    val nullArtId    = """{"language":"nb","metaImage":null}"""
    val nullExpected = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Delete)

    val existingArtId = """{"language":"nb","metaImage":{"id":"123","alt":"alt123"}}"""
    val existingExpected = TestData.emptyApiUpdatedConcept
      .copy(language = "nb", metaImage = UpdateWith(NewConceptMetaImage(id = "123", alt = "alt123")))

    {
      simpleHttpClient
        .send(
          quickRequest
            .patch(uri"http://localhost:$serverPort/concept-api/v1/drafts/1")
            .body(missing)
            .header("Authorization", TestData.authHeaderWithWriteRole)
        )
        .code
        .code should be(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(missingExpected), any[TokenUser])
    }

    {
      simpleHttpClient
        .send(
          quickRequest
            .patch(uri"http://localhost:$serverPort/concept-api/v1/drafts/1")
            .body(nullArtId)
            .header("Authorization", TestData.authHeaderWithWriteRole)
        )
        .code
        .code should be(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(nullExpected), any[TokenUser])
    }

    {
      simpleHttpClient
        .send(
          quickRequest
            .patch(uri"http://localhost:$serverPort/concept-api/v1/drafts/1")
            .body(existingArtId)
            .header("Authorization", TestData.authHeaderWithWriteRole)
        )
        .code
        .code should be(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(existingExpected), any[TokenUser])
    }
  }

  test("tags should return 200 OK if the result was not empty") {
    when(readService.getAllTags(anyString, anyInt, anyInt, anyString))
      .thenReturn(TestData.sampleApiTagsSearchResult)

    simpleHttpClient
      .send(quickRequest.get(uri"http://localhost:$serverPort/concept-api/v1/drafts/tag-search/"))
      .code
      .code should be(200)
  }

  test(
    "PATCH / should return 200 on updated, checking json4s deserializer of Either[Null, Option[NewConceptMetaImage]]"
  ) {
    reset(writeService)
    when(
      writeService
        .updateConcept(eqTo(1.toLong), any[UpdatedConcept], any[TokenUser])
    )
      .thenReturn(Success(TestData.sampleNbApiConcept))

    val missing         = """{"language":"nb"}"""
    val missingExpected = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Missing)

    val nullArtId    = """{"language":"nb","metaImage":null}"""
    val nullExpected = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Delete)

    val existingArtId = """{"language":"nb","metaImage": {"id": "1",
                          |		"alt": "alt-text"}}""".stripMargin
    val existingExpected = TestData.emptyApiUpdatedConcept
      .copy(language = "nb", metaImage = UpdateWith(api.NewConceptMetaImage("1", "alt-text")))

    {
      simpleHttpClient
        .send(
          quickRequest
            .patch(uri"http://localhost:$serverPort/concept-api/v1/drafts/1")
            .body(missing)
            .header("Authorization", TestData.authHeaderWithWriteRole)
        )
        .code
        .code should be(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(missingExpected), any[TokenUser])
    }

    {
      simpleHttpClient
        .send(
          quickRequest
            .patch(uri"http://localhost:$serverPort/concept-api/v1/drafts/1")
            .body(nullArtId)
            .header("Authorization", TestData.authHeaderWithWriteRole)
        )
        .code
        .code should be(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(nullExpected), any[TokenUser])
    }

    {
      simpleHttpClient
        .send(
          quickRequest
            .patch(uri"http://localhost:$serverPort/concept-api/v1/drafts/1")
            .body(existingArtId)
            .header("Authorization", TestData.authHeaderWithWriteRole)
        )
        .code
        .code should be(200)
      verify(writeService, times(1)).updateConcept(eqTo(1), eqTo(existingExpected), any[TokenUser])
    }
  }

  test("that scrolling doesn't happen on 'initial'") {
    reset(draftConceptSearchService)

    val multiResult =
      SearchResult[ConceptSummary](0, None, 10, "nn", Seq.empty, Seq.empty, Some("heiheihei"))
    when(draftConceptSearchService.all(any[search.DraftSearchSettings])).thenReturn(Success(multiResult))
    when(searchConverterService.asApiConceptSearchResult(any)).thenCallRealMethod()

    val expectedSettings =
      draftSearchSettings.empty.copy(shouldScroll = true, pageSize = 10, sort = Sort.ByTitleDesc)

    simpleHttpClient
      .send(
        quickRequest
          .get(uri"http://localhost:$serverPort/concept-api/v1/drafts/?search-context=initial")
          .header("Authorization", TestData.authHeaderWithWriteRole)
      )
      .code
      .code should be(200)

    verify(draftConceptSearchService, times(1)).all(eqTo(expectedSettings))
  }
}
