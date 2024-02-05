/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.common.model.domain
import no.ndla.draftapi.model.api.UploadedFile
import no.ndla.draftapi.{Eff, TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.Service
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.read
import org.scalatra.test.BytesPart
import sttp.client3.quick._

import scala.util.Success

class FileControllerTest extends UnitSuite with TestEnvironment {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  val serverPort: Int = findFreePort

  val controller                       = new FileController
  override val services: List[Service[Eff]] = List(controller)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer(this.getClass.getName, serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  override def beforeEach(): Unit = {
    reset(clock)
    when(clock.now()).thenCallRealMethod()
  }

  val exampleFile                  = BytesPart("hello.pdf", "Hello".getBytes, "application/pdf")
  val exampleFileBody: Array[Byte] = "Hello".getBytes

  test("That uploading a file returns 200 with body if successful") {
    val uploaded = UploadedFile("pwqofkpowegjw.pdf", "application/pdf", ".pdf", "files/resources/pwqofkpowegjw.pdf")
    when(writeService.storeFile(any[domain.UploadedFile])).thenReturn(Success(uploaded))

    val resp = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/draft-api/v1/files")
        .multipartBody[Any](multipart("file", exampleFileBody))
        .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
    )

    resp.code.code should be(200)
    read[UploadedFile](resp.body) should be(uploaded)
  }

//  test("That uploading a file fails with 400 if no file is specified") {
//    reset(writeService)
//    val resp = simpleHttpClient.send(
//      quickRequest
//        .post(uri"http://localhost:$serverPort/draft-api/v1/files")
//        .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
//    )
//
//    resp.code.code should be(400)
//    verify(writeService, times(0)).storeFile(any[domain.UploadedFile])
//  }

  test("That uploading a file requires publishing rights") {

    val resp = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/draft-api/v1/files")
        .multipartBody[Any](multipart("file", exampleFileBody))
    )
    resp.code.code should be(401)
  }
}
