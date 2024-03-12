/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.{NDLADate, api as commonApi}
import no.ndla.imageapi.model.api
import no.ndla.imageapi.model.api.{ImageAltText, ImageCaption, ImageTag, ImageTitle}
import no.ndla.imageapi.model.domain.{ImageFileData, ImageMetaInformation, ModelReleasedStatus}
import no.ndla.imageapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.mapping.License.{CC_BY, getLicense}
import no.ndla.tapirtesting.TapirControllerTest
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import org.json4s.jackson.Serialization.*
import sttp.client3.quick.*

import scala.util.{Failure, Success}
import no.ndla.mapping.LicenseDefinition
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{doReturn, never, reset, verify, verifyNoMoreInteractions, when}

class InternControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {
  override val converterService    = new ConverterService
  val controller: InternController = new InternController

  val updated: NDLADate       = NDLADate.of(2017, 4, 1, 12, 15, 32)
  val BySa: LicenseDefinition = getLicense(CC_BY.toString).get

  val DefaultApiImageMetaInformation: api.ImageMetaInformationV2 = api.ImageMetaInformationV2(
    "1",
    s"${props.ImageApiV2UrlBase}1",
    ImageTitle("", "nb"),
    ImageAltText("", "nb"),
    s"${props.RawImageUrlBase}/test.jpg",
    0,
    "",
    commonApi.Copyright(
      commonApi.License(BySa.license.toString, Some(BySa.description), BySa.url),
      None,
      List(),
      List(),
      List(),
      None,
      None,
      false
    ),
    ImageTag(Seq.empty, "nb"),
    ImageCaption("", "nb"),
    Seq("und"),
    updated,
    "ndla124",
    ModelReleasedStatus.YES.toString,
    None,
    None
  )

  val DefaultDomainImageMetaInformation = new ImageMetaInformation(
    id = Some(1),
    titles = List(),
    alttexts = List(),
    images = Seq(
      new ImageFileData(
        id = 1,
        fileName = "test.jpg",
        size = 0,
        contentType = "",
        dimensions = None,
        language = "und",
        imageMetaId = 1
      )
    ),
    copyright = Copyright(CC_BY.toString, None, List(), List(), List(), None, None, false),
    tags = List(),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  override def beforeEach(): Unit = {
    reset(clock)
    reset(imageRepository)
    reset(imageIndexService)
    when(clock.now()).thenCallRealMethod()
  }

  test("That GET /extern/abc returns 404") {
    when(imageRepository.withExternalId(eqTo("abc"))).thenReturn(None)
    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/intern/extern/abc")
      )
      .code
      .code should be(404)
  }

  test("That GET /extern/123 returns 404 if 123 is not found") {
    when(imageRepository.withExternalId(eqTo("123"))).thenReturn(None)
    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/intern/extern/123")
      )
      .code
      .code should be(404)
  }

  test("That GET /extern/123 returns 200 and imagemeta when found") {
    implicit val formats: Formats = org.json4s.DefaultFormats ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer

    when(imageRepository.withExternalId(eqTo("123"))).thenReturn(Some(DefaultDomainImageMetaInformation))
    val res = simpleHttpClient
      .send(quickRequest.get(uri"http://localhost:$serverPort/intern/extern/123"))
    res.code.code should be(200)
    res.body should equal(write(DefaultApiImageMetaInformation))
  }

  test("That DELETE /index removes all indexes") {
    when(imageIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index3"))
    val res = simpleHttpClient
      .send(quickRequest.delete(uri"http://localhost:$serverPort/intern/index"))
    res.code.code should be(200)
    res.body should be("Deleted 3 indexes")
    verify(imageIndexService).findAllIndexes(props.SearchIndex)
    verify(imageIndexService).deleteIndexWithName(Some("index1"))
    verify(imageIndexService).deleteIndexWithName(Some("index2"))
    verify(imageIndexService).deleteIndexWithName(Some("index3"))
    verifyNoMoreInteractions(imageIndexService)
  }

  test("That DELETE /index fails if at least one index isn't found, and no indexes are deleted") {
    doReturn(Failure(new RuntimeException("Failed to find indexes")), Nil: _*)
      .when(imageIndexService)
      .findAllIndexes(props.SearchIndex)
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index3"))
    val res = simpleHttpClient
      .send(quickRequest.delete(uri"http://localhost:$serverPort/intern/index"))
    res.code.code should equal(500)
    res.body should equal("Failed to find indexes")
    verify(imageIndexService, never).deleteIndexWithName(any[Option[String]])
  }

  test(
    "That DELETE /index fails if at least one index couldn't be deleted, but the other indexes are deleted regardless"
  ) {
    when(imageIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Failure(new RuntimeException("No index with name 'index2' exists")), Nil: _*)
      .when(imageIndexService)
      .deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index3"))
    val res = simpleHttpClient
      .send(quickRequest.delete(uri"http://localhost:$serverPort/intern/index"))
    res.code.code should equal(500)
    res.body should equal(
      "Failed to delete 1 index: No index with name 'index2' exists. 2 indexes were deleted successfully."
    )
    verify(imageIndexService).deleteIndexWithName(Some("index1"))
    verify(imageIndexService).deleteIndexWithName(Some("index2"))
    verify(imageIndexService).deleteIndexWithName(Some("index3"))
  }
}
