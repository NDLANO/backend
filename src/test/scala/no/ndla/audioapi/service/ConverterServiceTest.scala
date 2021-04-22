/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._
import no.ndla.mapping.License.CC_BY_SA

import scala.util.Success

class ConverterServiceTest extends UnitSuite with TestEnvironment {
  val service = new ConverterService

  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val copyrighted =
    Copyright("copyrighted", Some("New York"), Seq(Author("Forfatter", "Clark Kent")), Seq(), Seq(), None, None, None)

  val audioMeta = AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(Title("Batmen er på vift med en bil", "nb")),
    Seq(Audio("file.mp3", "audio/mpeg", 1024, "nb")),
    copyrighted,
    Seq(Tag(Seq("fisk"), "nb")),
    "ndla124",
    updated,
    Seq.empty,
    AudioType.Standard,
    Seq.empty,
    None
  )

  test("that toApiAudioMetaInformation converts a domain class to an api class") {

    val expected = api.AudioMetaInformation(
      audioMeta.id.get,
      audioMeta.revision.get,
      api.Title("Batmen er på vift med en bil", "nb"),
      service.toApiAudio(audioMeta.filePaths.headOption),
      service.toApiCopyright(audioMeta.copyright),
      api.Tag(Seq("fisk"), "nb"),
      Seq("nb"),
      "standard",
      None,
      None
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
      Seq("nb"),
      "standard",
      None,
      None
    )

    val expectedNoTitles = expectedDefaultLanguage.copy(title = api.Title("", "nb"))

    val audioWithNoTitles = audioMeta.copy(titles = Seq.empty)
    val randomLanguage = "norsk"

    service.toApiAudioMetaInformation(audioMeta, Some(randomLanguage)) should equal(Success(expectedDefaultLanguage))
    service.toApiAudioMetaInformation(audioWithNoTitles, Some(randomLanguage)) should equal(Success(expectedNoTitles))
  }

  test("That toApiLicense converts to an api.License") {
    val licenseAbbr = CC_BY_SA.toString
    val license = api.License(licenseAbbr,
                              Some("Creative Commons Attribution-ShareAlike 4.0 International"),
                              Some("https://creativecommons.org/licenses/by-sa/4.0/"))

    service.toApiLicence(licenseAbbr) should equal(license)
  }

  test("That toApiLicense returns unknown if the license is invalid") {
    val licenseAbbr = "garbage"

    service.toApiLicence(licenseAbbr) should equal(api.License("unknown", None, None))
  }

  test("That withAgreementCopyright returns with copyright") {
    val meta = audioMeta.copy(
      copyright = audioMeta.copyright.copy(agreementId = Some(1), processors = Seq(Author("Linguistic", "Tommy Test"))))
    val today = new DateTime().toDate()
    val agreementCopyright = api.Copyright(
      license = api.License("gnu", None, None),
      origin = Some("Originstuff"),
      creators = Seq(api.Author("Originator", "Christian Traktor")),
      processors = Seq(),
      rightsholders = Seq(api.Author("Publisher", "Marius Muffins")),
      agreementId = None,
      validFrom = Some(today),
      validTo = None
    )

    when(draftApiClient.getAgreementCopyright(1)).thenReturn(Some(agreementCopyright))
    val result = service.withAgreementCopyright(meta)
    result.copyright.license should equal("gnu")
    result.copyright.creators.head.name should equal("Christian Traktor")
    result.copyright.processors.head.name should equal("Tommy Test")
    result.copyright.rightsholders.head.name should equal("Marius Muffins")
    result.copyright.validFrom should equal(Some(today))
  }

  test("That withAgreementCopyright doesnt change anything if no agreement found") {
    val meta = audioMeta.copy(
      copyright = audioMeta.copyright.copy(agreementId = None, processors = Seq(Author("Linguistic", "Tommy Test"))))
    val result = service.withAgreementCopyright(meta)
    result should equal(meta)
  }

  test("That api version of withAgreementCopyright returns with copyright") {
    val copyright = service.toApiCopyright(
      audioMeta.copyright.copy(agreementId = Some(1), processors = Seq(Author("Linguistic", "Tommy Test"))))
    val today = new DateTime().toDate()
    val agreementCopyright = api.Copyright(
      license = api.License("gnu", None, None),
      origin = Some("Originstuff"),
      creators = Seq(api.Author("Originator", "Christian Traktor")),
      processors = Seq(),
      rightsholders = Seq(api.Author("Publisher", "Marius Muffins")),
      agreementId = None,
      validFrom = Some(today),
      validTo = None
    )

    when(draftApiClient.getAgreementCopyright(1)).thenReturn(Some(agreementCopyright))
    val result = service.withAgreementCopyright(copyright)
    result.license.license should equal("gnu")
    result.creators.head.name should equal("Christian Traktor")
    result.processors.head.name should equal("Tommy Test")
    result.rightsholders.head.name should equal("Marius Muffins")
    result.validFrom should equal(Some(today))
  }

  test("That api version of withAgreementCopyright doesnt change anything if no agreement found") {
    val copyright = service.toApiCopyright(
      audioMeta.copyright.copy(agreementId = None, processors = Seq(Author("Linguistic", "Tommy Test"))))
    val result = service.withAgreementCopyright(copyright)
    result should equal(copyright)
  }

  test("That mergeLanguageField merges language fields as expected") {
    val existingTitles = Seq(domain.Title("Tittel", "nb"), domain.Title("Title", "en"))

    val res1 = service.mergeLanguageField(existingTitles, domain.Title("Ny tittel", "nb"))
    val expected1 = Seq(domain.Title("Ny tittel", "nb"), domain.Title("Title", "en"))
    res1 should be(expected1)

    val res2 = service.mergeLanguageField(existingTitles, domain.Title("Ny tittel", "nn"))
    val expected2 = Seq(domain.Title("Tittel", "nb"), domain.Title("Title", "en"), domain.Title("Ny tittel", "nn"))
    res2 should be(expected2)
  }

  test("That mergeLanguageField deletes language fields as expected") {
    val existingTitles = Seq(domain.Title("Tittel", "nb"), domain.Title("Title", "en"))

    val res1 = service.mergeLanguageField(existingTitles, Some(domain.Title("Ny tittel", "nb")), "nb")
    val expected1 = Seq(domain.Title("Ny tittel", "nb"), domain.Title("Title", "en"))
    res1 should be(expected1)

    val res2 = service.mergeLanguageField(existingTitles, Some(domain.Title("Ny tittel", "nn")), "nn")
    val expected2 = Seq(domain.Title("Tittel", "nb"), domain.Title("Title", "en"), domain.Title("Ny tittel", "nn"))
    res2 should be(expected2)

    val res3 = service.mergeLanguageField(existingTitles, None, "en")
    val expected3 = Seq(domain.Title("Tittel", "nb"))
    res3 should be(expected3)
  }

}
