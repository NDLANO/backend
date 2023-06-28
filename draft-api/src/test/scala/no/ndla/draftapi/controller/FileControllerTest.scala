/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.model.api.UploadedFile
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.read
import org.scalatra.servlet.FileItem
import org.scalatra.test.BytesPart
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.Success

class FileControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  implicit val swagger: DraftSwagger        = new DraftSwagger

  lazy val controller = new FileController
  addServlet(controller, "/test")
  val exampleFile = BytesPart("hello.pdf", "Hello".getBytes, "application/pdf")

  test("That uploading a file returns 200 with body if successful") {
    val uploaded = UploadedFile("pwqofkpowegjw.pdf", "application/pdf", ".pdf", "files/resources/pwqofkpowegjw.pdf")
    when(writeService.storeFile(any[FileItem])).thenReturn(Success(uploaded))

    post(
      "/test",
      params = List.empty,
      files = List("file" -> exampleFile),
      headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)
    ) {
      status should be(200)
      read[UploadedFile](body) should be(uploaded)
    }
  }

  test("That uploading a file fails with 400 if no file is specified") {
    reset(writeService)
    post("/test", headers = Map("Authorization" -> TestData.authHeaderWithWriteRole)) {
      status should be(400)
      verify(writeService, times(0)).storeFile(any[FileItem])
    }
  }

  test("That uploading a file requires publishing rights") {
    post("/test", params = List.empty, files = List("file" -> exampleFile)) {
      status should be(403)
    }
  }
}
