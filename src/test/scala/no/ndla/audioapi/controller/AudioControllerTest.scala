/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.api._
import no.ndla.audioapi.{AudioSwagger, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatra.servlet.FileItem
import org.scalatra.test.Uploadable
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success}

class AudioControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  val legacyAuthHeaderWithWriteRole = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsiYXVkaW86d3JpdGUiXSwibmRsYV9pZCI6ImFiYzEyMyJ9LCJuYW1lIjoiRG9uYWxkIER1Y2siLCJpc3MiOiJodHRwczovL3NvbWUtZG9tYWluLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTIzIiwiYXVkIjoiYWJjIiwiZXhwIjoxNDg2MDcwMDYzLCJpYXQiOjE0ODYwMzQwNjN9.ZV7qJ4l-88JQkG_8beUyGbC-9Qdg0Um__SFtYlIOiiU"
  val legacyAuthHeaderWithoutAnyRoles = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOltdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30.kXjaQ9QudcRHTqhfrzKr0Zr4pYISBfJoXWHVBreDyO8"
  val legacyAuthHeaderWithWrongRole = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsic29tZTpvdGhlciJdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30.JsxMW8y0hCmpuu9tpQr6ZdfcqkOS8hRatFi3cTO_PvY"

  val authHeaderWithWriteRole = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXVkaW86d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.YYRWLfDDfnyyw6mDoOsvYEJtHf3uoJlkCUMmLKV1lXI"
  val authHeaderWithoutAnyRoles = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.6umgx7Xu8cnoBsry1NGL0iBe32wUuqCpLrospDlLmVc"
  val authHeaderWithWrongRole = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoic29tZTpvdGhlciIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.Hbmh9KX19nx7yT3rEcP9pyzRO0uQJBRucfqH9QEZtLyXjYj_fAyOhsoicOVEbHSES7rtdiJK43-gijSpWWmGWOkE6Ym7nHGhB_nLdvp_25PDgdKHo-KawZdAyIcJFr5_t3CJ2Z2IPVbrXwUd99vuXEBaV0dMwkT0kDtkwHuS-8E"

  implicit val swagger = new AudioSwagger
  lazy val controller = new AudioController
  addServlet(controller, "/*")

  val sampleUploadFile = new Uploadable {
    override def contentLength = 3
    override def content = Array[Byte](0x49, 0x44, 0x33)
    override def contentType = "audio/mp3"
    override def fileName = "test.mp3"
  }
  val sampleNewAudioMeta =
    """
      |{
      |    "title": "Test",
      |    "language": "nb",
      |    "audioFile": "test.mp3",
      |    "copyright": {
      |        "license": {
      |            "license": "by-sa"
      |        },
      |        "authors": []
      |    },
      |    "tags": ["test"]
      |}
    """.stripMargin

  test("That POST / returns 403 if no auth-header") {
    post("/", Map("metadata" -> sampleNewAudioMeta)) {
      status should equal (403)
    }
  }

  test("That POST / returns 400 if parameters are missing") {
    post("/", Map("metadata" -> sampleNewAudioMeta), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal (400)
    }
  }

  test("That POST / returns 200 if everything is fine and dandy") {
    val sampleAudioMeta = AudioMetaInformation(1, 1, Title("title", "nb"), Audio("", "", -1, "nb"), Copyright(License("by", None, None), None, Seq(), Seq(), Seq(), None, None, None), Tag(Seq(), "nb"), Seq("nb"))
    when(writeService.storeNewAudio(any[NewAudioMetaInformation], any[FileItem])).thenReturn(Success(sampleAudioMeta))

    post("/", Map("metadata" -> sampleNewAudioMeta), Map("file" -> sampleUploadFile), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal (200)
    }
  }

  test("That POST / returns 500 if an unexpected error occurs") {
    when(writeService.storeNewAudio(any[NewAudioMetaInformation], any[FileItem])).thenReturn(Failure(mock[RuntimeException]))

    post("/", Map("metadata" -> sampleNewAudioMeta), Map("file" -> sampleUploadFile), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal (500)
    }
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    post("/", Map("metadata" -> sampleNewAudioMeta), headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    post("/", Map("metadata" -> sampleNewAudioMeta), headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }

  // Legacy tests. May be removed when the legacy token format in ndla.network v0.24 is removed
  test("LEGACY - That POST / returns 400 if parameters are missing") {
    post("/", Map("metadata" -> sampleNewAudioMeta), headers = Map("Authorization" -> legacyAuthHeaderWithWriteRole)) {
      status should equal (400)
    }
  }

  test("LEGACY - That POST / returns 200 if everything is fine and dandy") {
    val sampleAudioMeta = AudioMetaInformation(1, 1, Title("title", "nb"), Audio("", "", -1, "nb"), Copyright(License("by", None, None), None, Seq(), Seq(), Seq(), None, None, None), Tag(Seq(), "nb"), Seq("nb"))
    when(writeService.storeNewAudio(any[NewAudioMetaInformation], any[FileItem])).thenReturn(Success(sampleAudioMeta))

    post("/", Map("metadata" -> sampleNewAudioMeta), Map("file" -> sampleUploadFile), headers = Map("Authorization" -> legacyAuthHeaderWithWriteRole)) {
      status should equal (200)
    }
  }

  test("LEGACY - That POST / returns 500 if an unexpected error occurs") {
    when(writeService.storeNewAudio(any[NewAudioMetaInformation], any[FileItem])).thenReturn(Failure(mock[RuntimeException]))

    post("/", Map("metadata" -> sampleNewAudioMeta), Map("file" -> sampleUploadFile), headers = Map("Authorization" -> legacyAuthHeaderWithWriteRole)) {
      status should equal (500)
    }
  }

  test("LEGACY - That POST / returns 403 if auth header does not have expected role") {
    post("/", Map("metadata" -> sampleNewAudioMeta), headers = Map("Authorization" -> legacyAuthHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("LEGACY - That POST / returns 403 if auth header does not have any roles") {
    post("/", Map("metadata" -> sampleNewAudioMeta), headers = Map("Authorization" -> legacyAuthHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }
}
