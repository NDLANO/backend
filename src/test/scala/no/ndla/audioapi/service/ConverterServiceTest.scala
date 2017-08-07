/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.model.api
import no.ndla.audioapi.model.domain.{Audio, AudioMetaInformation, Author, Copyright, Tag, Title, _}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._

import scala.util.{Failure, Success}

class ConverterServiceTest extends UnitSuite with TestEnvironment {
  val service = new ConverterService

  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate
  val copyrighted = Copyright("copyrighted", Some("New York"), Seq(Author("Forfatter", "Clark Kent")))
  val audioMeta = AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(Title("Batmen er på vift med en bil", "nb")),
    Seq(Audio("file.mp3", "audio/mpeg", 1024, "nb")),
    copyrighted,
    Seq(Tag(Seq("fisk"), "nb")),
    "ndla124",
    updated)

  test("that toApiAudioMetaInformation converts a domain class to an api class") {

    val expected = api.AudioMetaInformation(
      audioMeta.id.get,
      audioMeta.revision.get,
      api.Title("Batmen er på vift med en bil", "nb"),
      service.toApiAudio(audioMeta.filePaths.headOption),
      service.toApiCopyright(audioMeta.copyright),
      api.Tag(Seq("fisk"), "nb"),
      Seq("nb")
    )

    service.toApiAudioMetaInformation(audioMeta, Some("nb")) should equal(Success(expected))
  }

  test("that toApiAudioMetaInformation should return DefaultLanguage if language is not supported") {
    val expectedDefaultLanguage = api.AudioMetaInformation(
      audioMeta.id.get,
      audioMeta.revision.get,
      api.Title("Batmen er på vift med en bil", "nb"),
      service.toApiAudio(audioMeta.filePaths.headOption),
      service.toApiCopyright(audioMeta.copyright),
      api.Tag(Seq("fisk"), "nb"),
      Seq("nb")
    )

    val expectedNoTitles = expectedDefaultLanguage.copy(title = api.Title("", "nb"))


    val audioWithNoTitles = audioMeta.copy(titles = Seq.empty)
    val randomLanguage = "norsk"

    service.toApiAudioMetaInformation(audioMeta, Some(randomLanguage)) should equal(Success(expectedDefaultLanguage))
    service.toApiAudioMetaInformation(audioWithNoTitles, Some(randomLanguage)) should equal(Success(expectedNoTitles))
  }

  test("That toApiLicense converts to an api.License") {
    val licenseAbbr = "by-sa"
    val license = api.License(licenseAbbr, Some("Creative Commons Attribution-ShareAlike 2.0 Generic"), Some("https://creativecommons.org/licenses/by-sa/2.0/"))

    service.toApiLicence(licenseAbbr) should equal (license)
  }

  test("That toApiLicense returns unknown if the license is invalid") {
    val licenseAbbr = "garbage"

    service.toApiLicence(licenseAbbr) should equal (api.License("unknown", None, None))
  }
}
