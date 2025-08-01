/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.model.domain.*
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain.{Author, ContributorType, Tag, Title}
import no.ndla.common.model.{NDLADate, api as commonApi, domain as common}
import no.ndla.mapping.License
import no.ndla.mapping.License.CC_BY_SA

import scala.util.Success

class ConverterServiceTest extends UnitSuite with TestEnvironment {
  val service = new ConverterService

  val updated: NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)
  val created: NDLADate = NDLADate.of(2017, 3, 1, 12, 15, 32)

  val copyrighted: Copyright =
    Copyright(
      License.Copyrighted.toString,
      Some("New York"),
      Seq(Author(ContributorType.Writer, "Clark Kent")),
      Seq(),
      Seq(),
      None,
      None,
      false
    )

  val audioMeta: AudioMetaInformation = domain.AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(Title("Batmen er på vift med en bil", "nb")),
    Seq(Audio("file.mp3", "audio/mpeg", 1024, "nb")),
    copyrighted,
    Seq(Tag(Seq("fisk"), "nb")),
    "ndla124",
    updated,
    created,
    Seq.empty,
    AudioType.Standard,
    Seq.empty,
    None,
    None
  )

  test("that toApiAudioMetaInformation converts a domain class to an api class") {

    val expected = api.AudioMetaInformationDTO(
      audioMeta.id.get,
      audioMeta.revision.get,
      api.TitleDTO("Batmen er på vift med en bil", "nb"),
      service.toApiAudio(audioMeta.filePaths.headOption),
      service.toApiCopyright(audioMeta.copyright),
      api.TagDTO(Seq("fisk"), "nb"),
      Seq("nb"),
      "standard",
      None,
      None,
      None,
      created,
      updated
    )

    service.toApiAudioMetaInformation(audioMeta, Some("nb")) should equal(Success(expected))
  }

  test("that toApiAudioMetaInformation should return DefaultLanguage if language is not supported") {
    val expectedDefaultLanguage = api.AudioMetaInformationDTO(
      audioMeta.id.get,
      audioMeta.revision.get,
      api.TitleDTO("Batmen er på vift med en bil", "nb"),
      service.toApiAudio(audioMeta.filePaths.headOption),
      service.toApiCopyright(audioMeta.copyright),
      api.TagDTO(Seq("fisk"), "nb"),
      Seq("nb"),
      "standard",
      None,
      None,
      None,
      created,
      updated
    )

    val expectedNoTitles = expectedDefaultLanguage.copy(title = api.TitleDTO("", "nb"))

    val audioWithNoTitles = audioMeta.copy(titles = Seq.empty)
    val randomLanguage    = "norsk"

    service.toApiAudioMetaInformation(audioMeta, Some(randomLanguage)) should equal(Success(expectedDefaultLanguage))
    service.toApiAudioMetaInformation(audioWithNoTitles, Some(randomLanguage)) should equal(Success(expectedNoTitles))
  }

  test("That toApiLicense converts to an api.License") {
    val licenseAbbr = CC_BY_SA.toString
    val license     = commonApi.LicenseDTO(
      licenseAbbr,
      Some("Creative Commons Attribution-ShareAlike 4.0 International"),
      Some("https://creativecommons.org/licenses/by-sa/4.0/")
    )

    service.toApiLicence(licenseAbbr) should equal(license)
  }

  test("That toApiLicense returns unknown if the license is invalid") {
    val licenseAbbr = "garbage"

    service.toApiLicence(licenseAbbr) should equal(commonApi.LicenseDTO("unknown", None, None))
  }

  test("That mergeLanguageField merges language fields as expected") {
    val existingTitles = Seq(common.Title("Tittel", "nb"), common.Title("Title", "en"))

    val res1      = service.mergeLanguageField(existingTitles, common.Title("Ny tittel", "nb"))
    val expected1 = Seq(common.Title("Ny tittel", "nb"), common.Title("Title", "en"))
    res1 should be(expected1)

    val res2      = service.mergeLanguageField(existingTitles, common.Title("Ny tittel", "nn"))
    val expected2 = Seq(common.Title("Tittel", "nb"), common.Title("Title", "en"), common.Title("Ny tittel", "nn"))
    res2 should be(expected2)
  }

  test("That mergeLanguageField deletes language fields as expected") {
    val existingTitles = Seq(common.Title("Tittel", "nb"), common.Title("Title", "en"))

    val res1      = service.mergeLanguageField(existingTitles, Some(common.Title("Ny tittel", "nb")), "nb")
    val expected1 = Seq(common.Title("Ny tittel", "nb"), common.Title("Title", "en"))
    res1 should be(expected1)

    val res2      = service.mergeLanguageField(existingTitles, Some(common.Title("Ny tittel", "nn")), "nn")
    val expected2 = Seq(common.Title("Tittel", "nb"), common.Title("Title", "en"), common.Title("Ny tittel", "nn"))
    res2 should be(expected2)

    val res3      = service.mergeLanguageField(existingTitles, None, "en")
    val expected3 = Seq(common.Title("Tittel", "nb"))
    res3 should be(expected3)
  }

}
