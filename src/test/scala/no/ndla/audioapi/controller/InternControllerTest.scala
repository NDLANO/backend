/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.domain.{AudioMetaInformation, Tag}
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

  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val DefaultApiImageMetaInformation = api.AudioMetaInformation(1, "", "title", api.Audio("audio/test.mp3", "audio/mpeg", 1024), api.Copyright(api.License("by-sa", None, None), None, Seq()), Seq("nb"), Seq("tag"))
  val DefaultDomainImageMetaInformation = domain.AudioMetaInformation(Some(1), Seq(domain.Title("title", Some("nb"))), Seq(domain.Audio("audio/test.mp3", "audio/mpeg", 1024, Some("nb"))), domain.Copyright("by-sa", None, Seq()), Seq(Tag(Seq("tag"), Option("nb"))), "ndla124", updated)
  val DefaultDomainAudioNoLanguage = domain.AudioMetaInformation(Some(1), Seq(domain.Title("title", None)), Seq(domain.Audio("audio/test.mp3", "audio/mpeg", 1024, None)), domain.Copyright("by-sa", None, Seq()), Seq(Tag(Seq("tag"), None)), "ndla124", updated)

  test("That POST /import/123 returns 200 OK when import is a success") {
    when(importService.importAudio(eqTo("123"))).thenReturn(Success(DefaultDomainImageMetaInformation))
    when(searchIndexService.indexDocument(eqTo(DefaultDomainImageMetaInformation))).thenReturn(Success(DefaultDomainImageMetaInformation))
    post("/import/123") {
      status should equal (200)
    }
  }

  test("That POST /import/123 returns 200 OK when imported resource does not have a language") {
    when(importService.importAudio(eqTo("123"))).thenReturn(Success(DefaultDomainAudioNoLanguage))
    when(searchIndexService.indexDocument(eqTo(DefaultDomainAudioNoLanguage))).thenReturn(Success(DefaultDomainAudioNoLanguage))

    post("/import/123") {
      status should equal (200)
    }
  }

  test("That POST /import/123 returns 500 with error message when import failed") {
    when(importService.importAudio(eqTo("123"))).thenReturn(Failure(new NullPointerException("There was a nullpointer exception")))

    post("/import/123") {
      status should equal (500)
      body indexOf "nullpointer" should be > 0
    }
  }
}
