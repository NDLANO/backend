/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.model.api.{ImageAltText, ImageCaption, ImageTag, ImageTitle}
import no.ndla.imageapi.model.domain.{ImageFileData, ImageMetaInformation, ModelReleasedStatus}
import no.ndla.imageapi.model.{api, domain}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.mapping.License.{CC_BY, getLicense}
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import org.json4s.jackson.Serialization._
import org.scalatra.test.scalatest.ScalatraSuite

import java.time.LocalDateTime
import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  override val converterService = new ConverterService
  implicit val swagger          = new ImageSwagger
  lazy val controller           = new InternController
  addServlet(controller, "/*")
  val updated = LocalDateTime.of(2017, 4, 1, 12, 15, 32)

  val BySa = getLicense(CC_BY.toString).get

  val DefaultApiImageMetaInformation = api.ImageMetaInformationV2(
    "1",
    s"${props.ImageApiUrlBase}1",
    ImageTitle("", "nb"),
    ImageAltText("", "nb"),
    s"${props.RawImageUrlBase}/test.jpg",
    0,
    "",
    api.Copyright(
      api.License(BySa.license.toString, BySa.description, BySa.url),
      "",
      List(),
      List(),
      List(),
      None,
      None,
      None
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
    copyright = domain.Copyright(CC_BY.toString, "", List(), List(), List(), None, None, None),
    tags = List(),
    captions = List(),
    updatedBy = "ndla124",
    updated = updated,
    created = updated,
    createdBy = "ndla124",
    modelReleased = ModelReleasedStatus.YES,
    editorNotes = Seq.empty
  )

  override def beforeEach() = {
    reset(imageRepository, imageIndexService)
  }

  test("That GET /extern/abc returns 404") {
    when(imageRepository.withExternalId(eqTo("abc"))).thenReturn(None)
    get("/extern/abc") {
      status should equal(404)
    }
  }

  test("That GET /extern/123 returns 404 if 123 is not found") {
    when(imageRepository.withExternalId(eqTo("123"))).thenReturn(None)
    get("/extern/123") {
      status should equal(404)
    }
  }

  test("That GET /extern/123 returns 200 and imagemeta when found") {
    implicit val formats: Formats = org.json4s.DefaultFormats ++ JavaTimeSerializers.all

    when(imageRepository.withExternalId(eqTo("123"))).thenReturn(Some(DefaultDomainImageMetaInformation))
    get("/extern/123") {
      status should equal(200)
      body should equal(write(DefaultApiImageMetaInformation))
    }
  }

  test("That DELETE /index removes all indexes") {
    when(imageIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(imageIndexService).deleteIndexWithName(Some("index3"))
    delete("/index") {
      status should equal(200)
      body should equal("Deleted 3 indexes")
    }
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
    delete("/index") {
      status should equal(500)
      body should equal("Failed to find indexes")
    }
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
    delete("/index") {
      status should equal(500)
      body should equal(
        "Failed to delete 1 index: No index with name 'index2' exists. 2 indexes were deleted successfully."
      )
    }
    verify(imageIndexService).deleteIndexWithName(Some("index1"))
    verify(imageIndexService).deleteIndexWithName(Some("index2"))
    verify(imageIndexService).deleteIndexWithName(Some("index3"))
  }

}
