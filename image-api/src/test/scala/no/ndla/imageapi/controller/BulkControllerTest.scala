/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.errors.NotFoundException
import no.ndla.common.model.api.{AuthorDTO, CopyrightDTO, LicenseDTO}
import no.ndla.common.model.domain.{AiGenerated, ContributorType}
import no.ndla.common.{CirceUtil, Clock}
import no.ndla.imageapi.model.api.NewImageMetaInformationV2DTO
import no.ndla.imageapi.model.api.bulk.{
  BulkUploadItemDTO,
  BulkUploadItemStatus,
  BulkUploadStartedDTO,
  BulkUploadStateDTO,
  BulkUploadStatus,
}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import ox.flow.Flow
import sttp.client4.quick.*

import java.util.UUID
import scala.util.{Failure, Success}

class BulkControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock: Clock                    = new Clock
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  val controller: BulkController                             = new BulkController
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes

  private val authHeaderWithBatchRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJpbWFnZXM6YmF0Y2giLCJpbWFnZXM6d3JpdGUiXSwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.1_j9R9KML2LTqeAE4bpRByJcR6m6Tv3pTOozpYCnTC8"

  private val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6W10sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.vw9YhRtgUQr_vuDhLNHfBsZz-4XLhCc1Kwxi0w0_qGI"

  private val sampleCopyright: CopyrightDTO = CopyrightDTO(
    license = LicenseDTO("CC-BY-SA-4.0", None, None),
    origin = None,
    creators = Seq(AuthorDTO(ContributorType.Photographer, "Test Author")),
    processors = Seq.empty,
    rightsholders = Seq.empty,
    validFrom = None,
    validTo = None,
    processed = false,
  )

  private val sampleNewImageMetaDTO: NewImageMetaInformationV2DTO = NewImageMetaInformationV2DTO(
    title = "test1",
    alttext = Some("test2"),
    copyright = sampleCopyright,
    tags = Seq("lel"),
    caption = "captionheredude",
    language = "nb",
    modelReleased = None,
    aiGenerated = AiGenerated.No,
  )

  private val sampleNewImageMeta: String = CirceUtil.toJsonString(sampleNewImageMetaDTO)

  private val fileBody: Array[Byte] = Array[Byte](-1, -40, -1)

  private val baseUrl: String = s"http://localhost:$serverPort/image-api/v1/bulk"

  private def runningState(items: Int = 1): BulkUploadStateDTO = BulkUploadStateDTO(
    status = BulkUploadStatus.Running,
    total = items,
    completed = 0,
    failed = 0,
    items = List.fill(items)(BulkUploadItemDTO(Some("a.jpg"), BulkUploadItemStatus.Uploading, None, None)),
    error = None,
  )

  override def beforeEach(): Unit = {
    reset(readService)
    reset(writeService)
  }

  test("That POST / returns 401 when the auth header is missing") {
    val res = quickRequest
      .post(uri"$baseUrl")
      .multipartBody(multipart("metadatas", sampleNewImageMeta), multipart("files", fileBody))
      .send()
    res.code.code should be(401)
  }

  test("That POST / returns 403 when the auth header lacks the images:batch permission") {
    val res = quickRequest
      .post(uri"$baseUrl")
      .multipartBody(multipart("metadatas", sampleNewImageMeta), multipart("files", fileBody))
      .header("Authorization", authHeaderWithoutAnyRoles)
      .send()
    res.code.code should be(403)
  }

  test("That POST / returns 400 when the number of metadata parts does not match the number of file parts") {
    val res = quickRequest
      .post(uri"$baseUrl")
      .multipartBody(
        multipart("metadatas", sampleNewImageMeta),
        multipart("metadatas", sampleNewImageMeta),
        multipart("files", fileBody),
      )
      .header("Authorization", authHeaderWithBatchRole)
      .send()
    res.code.code should be(400)
  }

  test("That POST / returns 200 with the upload id when the upload is queued successfully") {
    when(writeService.batchStoreImages(any[UUID], any, any, any)).thenReturn(Success(()))

    val res = quickRequest
      .post(uri"$baseUrl")
      .multipartBody(multipart("metadatas", sampleNewImageMeta), multipart("files", fileBody))
      .header("Authorization", authHeaderWithBatchRole)
      .send()

    res.code.code should be(200)
    val started = CirceUtil.unsafeParseAs[BulkUploadStartedDTO](res.body)
    started.uploadId should not be null
  }

  test("That POST / returns 500 when writeService.batchStoreImages fails") {
    when(writeService.batchStoreImages(any[UUID], any, any, any)).thenReturn(
      Failure(new RuntimeException("redis down"))
    )

    val res = quickRequest
      .post(uri"$baseUrl")
      .multipartBody(multipart("metadatas", sampleNewImageMeta), multipart("files", fileBody))
      .header("Authorization", authHeaderWithBatchRole)
      .send()
    res.code.code should be(500)
  }

  test("That GET /status/<id> returns 401 when the auth header is missing") {
    val res = quickRequest.get(uri"$baseUrl/status/${UUID.randomUUID()}").send()
    res.code.code should be(401)
  }

  test("That GET /status/<id> returns 403 when the auth header lacks the images:batch permission") {
    val res = quickRequest
      .get(uri"$baseUrl/status/${UUID.randomUUID()}")
      .header("Authorization", authHeaderWithoutAnyRoles)
      .send()
    res.code.code should be(403)
  }

  test("That GET /status/<id> returns 404 when the upload id is unknown") {
    val uploadId = UUID.randomUUID()
    when(readService.getStatusStreamOfBulkUpload(eqTo(uploadId))).thenReturn(
      Failure(NotFoundException(s"No bulk upload with id $uploadId"))
    )

    val res = quickRequest.get(uri"$baseUrl/status/$uploadId").header("Authorization", authHeaderWithBatchRole).send()
    res.code.code should be(404)
  }

  test("That GET /status/<id> streams every state from the flow as an SSE event with content-type text/event-stream") {
    val uploadId = UUID.randomUUID()
    val running  = runningState()
    val complete = running.copy(
      status = BulkUploadStatus.Complete,
      completed = 1,
      items = running.items.map(_.copy(status = BulkUploadItemStatus.Done)),
    )
    when(readService.getStatusStreamOfBulkUpload(eqTo(uploadId))).thenReturn(
      Success(Flow.fromValues(running, complete))
    )

    val res = quickRequest.get(uri"$baseUrl/status/$uploadId").header("Authorization", authHeaderWithBatchRole).send()

    res.code.code should be(200)
    res.contentType.exists(_.startsWith("text/event-stream")) should be(true)

    val events = parseSseEvents(res.body)
    events should have size 2

    events.head.eventType should be(Some("Progress"))
    CirceUtil.unsafeParseAs[BulkUploadStateDTO](events.head.data) should be(running)

    events(1).eventType should be(Some("Complete"))
    CirceUtil.unsafeParseAs[BulkUploadStateDTO](events(1).data) should be(complete)
  }

  test("That GET /status/<id> emits the Failed event type when the terminal state is Failed") {
    val uploadId = UUID.randomUUID()
    val failed   = runningState().copy(
      status = BulkUploadStatus.Failed,
      failed = 1,
      items = List(BulkUploadItemDTO(Some("a.jpg"), BulkUploadItemStatus.Failed, None, Some("boom"))),
      error = Some("boom"),
    )
    when(readService.getStatusStreamOfBulkUpload(eqTo(uploadId))).thenReturn(Success(Flow.fromValues(failed)))

    val res = quickRequest.get(uri"$baseUrl/status/$uploadId").header("Authorization", authHeaderWithBatchRole).send()

    res.code.code should be(200)
    val events = parseSseEvents(res.body)
    events should have size 1
    events.head.eventType should be(Some("Failed"))
  }

  test("That GET /status/<id> returns 200 with no events when the flow is empty") {
    val uploadId = UUID.randomUUID()
    when(readService.getStatusStreamOfBulkUpload(eqTo(uploadId))).thenReturn(Success(Flow.empty[BulkUploadStateDTO]))

    val res = quickRequest.get(uri"$baseUrl/status/$uploadId").header("Authorization", authHeaderWithBatchRole).send()

    res.code.code should be(200)
    parseSseEvents(res.body) should be(empty)
  }

  private case class SseEvent(eventType: Option[String], data: String)

  /** Minimal SSE parser: groups consecutive non-blank lines into events, extracting the `event:` and `data:` fields. */
  private def parseSseEvents(body: String): List[SseEvent] = {
    body
      .split("\r?\n\r?\n")
      .toList
      .filter(_.trim.nonEmpty)
      .map { block =>
        val lines     = block.linesIterator.toList
        val eventType = lines.collectFirst {
          case l if l.startsWith("event:") => l.stripPrefix("event:").trim
        }
        val data = lines
          .collect {
            case l if l.startsWith("data:") => l.stripPrefix("data:").trim
          }
          .mkString("\n")
        SseEvent(eventType, data)
      }
  }
}
