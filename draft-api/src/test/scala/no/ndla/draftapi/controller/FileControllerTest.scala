/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.controller

import no.ndla.common.{CirceUtil, Clock}
import no.ndla.common.model.domain
import no.ndla.draftapi.model.api.UploadedFileDTO
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import sttp.client3.quick.*

import scala.util.Success

class FileControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override val controller: FileController                    = new FileController
  override implicit lazy val clock: Clock                    = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes

  override def beforeEach(): Unit = {
    reset(clock)
    when(clock.now()).thenCallRealMethod()
  }

  val exampleFileBody: Array[Byte] = "Hello".getBytes

  test("That uploading a file returns 200 with body if successful") {
    val uploaded = UploadedFileDTO("pwqofkpowegjw.pdf", "application/pdf", ".pdf", "files/resources/pwqofkpowegjw.pdf")
    when(writeService.storeFile(any[domain.UploadedFile])).thenReturn(Success(uploaded))

    val resp = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/draft-api/v1/files")
        .multipartBody[Any](multipart("file", exampleFileBody))
        .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
    )

    resp.code.code should be(200)
    CirceUtil.unsafeParseAs[UploadedFileDTO](resp.body) should be(uploaded)
  }

  test("That uploading a file fails with 400 if no file is specified") {
    reset(writeService)
    val resp = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/draft-api/v1/files")
        .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
    )

    resp.code.code should be(400)
    verify(writeService, times(0)).storeFile(any[domain.UploadedFile])
  }

  test("That uploading a file requires publishing rights") {

    val resp = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/draft-api/v1/files")
        .multipartBody[Any](multipart("file", exampleFileBody))
    )
    resp.code.code should be(401)
  }
}
