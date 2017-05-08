/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  override val converterService = new ConverterService
  lazy val controller = new InternController
  addServlet(controller, "/*")

  def updated() = (new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate

  val DefaultApiImageMetaInformation = api.AudioMetaInformation(1, Seq(api.Title("title", Some("nb"))), Seq(api.Audio("audio/test.mp3", "audio/mpeg", 1024, Some("nb"))), api.Copyright(api.License("by-sa", None, None), None, Seq()), Seq())
  val DefaultDomainImageMetaInformation = domain.AudioMetaInformation(Some(1), Seq(domain.Title("title", Some("nb"))), Seq(domain.Audio("audio/test.mp3", "audio/mpeg", 1024, Some("nb"))), domain.Copyright("by-sa", None, Seq()), Seq(), "ndla124", updated())

  test("That POST /import/123 returns 200 OK when import is a success") {
    when(importService.importAudio(eqTo("123"))).thenReturn(Success(DefaultDomainImageMetaInformation))
    when(searchIndexService.indexDocument(eqTo(DefaultDomainImageMetaInformation))).thenReturn(Success(DefaultDomainImageMetaInformation))
    post("/import/123") {
      status should equal (200)
    }
  }

  test("That POST /import/123 returns 500 with error message when import failed") {
    when(importService.importAudio(eqTo("123"))).thenReturn(Failure(new NullPointerException("null")))

    post("/import/123") {
      status should equal (500)
      body indexOf "audio with external_id" should be > 0
    }
  }
}
