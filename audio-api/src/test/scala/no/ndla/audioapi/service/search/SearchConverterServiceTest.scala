/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import no.ndla.audioapi.model.domain.*
import no.ndla.audioapi.model.search.SearchableAudioInformation
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain.{Author, Tag, Title}
import no.ndla.mapping.License
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

class SearchConverterServiceTest extends UnitSuite with TestEnvironment {

  override val searchConverterService = new SearchConverterService

  val byNcSa: Copyright =
    Copyright(
      License.CC_BY_NC_SA.toString,
      Some("Gotham City"),
      List(Author("Forfatter", "DC Comics")),
      Seq(),
      Seq(),
      None,
      None,
      false
    )
  def updated(): NDLADate = NDLADate.of(2017, 4, 1, 12, 15, 32)
  def created(): NDLADate = NDLADate.of(2017, 3, 1, 12, 15, 32)

  val domainTitles: List[Title] = List(
    Title("Bokmål tittel", "nb"),
    Title("Nynorsk tittel", "nn"),
    Title("English title", "en"),
    Title("Titre francais", "fr"),
    Title("Deutsch titel", "de"),
    Title("Titulo espanol", "es"),
    Title("Nekonata titolo", "unknown")
  )

  val apiTitles: List[api.TitleDTO] = List(
    api.TitleDTO("Bokmål tittel", "nb"),
    api.TitleDTO("Nynorsk tittel", "nn"),
    api.TitleDTO("English title", "en"),
    api.TitleDTO("Titre francais", "fr"),
    api.TitleDTO("Deutsch titel", "de"),
    api.TitleDTO("Titulo espanol", "es"),
    api.TitleDTO("Nekonata titolo", "unknown")
  )

  val audioFiles: Seq[Audio] = Seq(
    Audio("file.mp3", "audio/mpeg", 1024, "nb"),
    Audio("file2.mp3", "audio/mpeg", 2048, "nb"),
    Audio("file3.mp3", "audio/mpeg", 4096, "nb"),
    Audio("file4.mp3", "audio/mpeg", 8192, "nb")
  )

  val audioTags: Seq[Tag] = Seq(
    Tag(Seq("fugl", "fisk"), "nb"),
    Tag(Seq("fugl", "fisk"), "nn"),
    Tag(Seq("bird", "fish"), "en"),
    Tag(Seq("got", "tired"), "fr"),
    Tag(Seq("of", "translating"), "de"),
    Tag(Seq("all", "of"), "es"),
    Tag(Seq("the", "words"), "unknown")
  )

  val sampleAudio: domain.AudioMetaInformation =
    domain.AudioMetaInformation(
      Some(1),
      Some(1),
      domainTitles,
      audioFiles,
      byNcSa,
      audioTags,
      "ndla124",
      updated(),
      created(),
      Seq.empty,
      AudioType.Standard,
      Seq.empty,
      None,
      None
    )

  override def beforeAll(): Unit = { super.beforeAll() }

  test("That asSearchableAudioInformation converts titles with correct language") {
    val searchableAudio = searchConverterService.asSearchableAudioInformation(sampleAudio)
    verifyTitles(searchableAudio.get)
  }

  test("That asSearchableAudioInformation converts articles with correct language") {
    searchConverterService.asSearchableAudioInformation(sampleAudio)
  }

  test("That asSearchableAudioInformation converts tags with correct language") {
    val searchableAudio = searchConverterService.asSearchableAudioInformation(sampleAudio)
    verifyTags(searchableAudio.get)
  }

  test("That asSearchableAudioInformation converts all fields with correct language") {
    val searchableAudio = searchConverterService.asSearchableAudioInformation(sampleAudio)

    verifyTitles(searchableAudio.get)
    verifyTags(searchableAudio.get)
  }

  private def verifyTitles(searchableAudio: SearchableAudioInformation): Unit = {
    searchableAudio.titles.languageValues.size should equal(domainTitles.size)
    languageValueWithLang(searchableAudio.titles, "nb") should equal(titleForLang(domainTitles, "nb"))
    languageValueWithLang(searchableAudio.titles, "nn") should equal(titleForLang(domainTitles, "nn"))
    languageValueWithLang(searchableAudio.titles, "en") should equal(titleForLang(domainTitles, "en"))
    languageValueWithLang(searchableAudio.titles, "fr") should equal(titleForLang(domainTitles, "fr"))
    languageValueWithLang(searchableAudio.titles, "de") should equal(titleForLang(domainTitles, "de"))
    languageValueWithLang(searchableAudio.titles, "es") should equal(titleForLang(domainTitles, "es"))
  }

  private def verifyTags(searchableAudio: SearchableAudioInformation): Unit = {
    languageListWithLang(searchableAudio.tags, "nb") should equal(tagsForLang(audioTags, "nb"))
    languageListWithLang(searchableAudio.tags, "nn") should equal(tagsForLang(audioTags, "nn"))
    languageListWithLang(searchableAudio.tags, "en") should equal(tagsForLang(audioTags, "en"))
    languageListWithLang(searchableAudio.tags, "fr") should equal(tagsForLang(audioTags, "fr"))
    languageListWithLang(searchableAudio.tags, "de") should equal(tagsForLang(audioTags, "de"))
    languageListWithLang(searchableAudio.tags, "es") should equal(tagsForLang(audioTags, "es"))
  }

  private def languageValueWithLang(languageValues: SearchableLanguageValues, lang: String): String = {
    languageValues.languageValues.find(_.language == lang).get.value
  }

  private def languageListWithLang(languageList: SearchableLanguageList, lang: String): Seq[String] = {
    languageList.languageValues.find(_.language == lang).get.value
  }

  private def titleForLang(titles: Seq[Title], lang: String): String = {
    titles.find(_.language == lang).get.title
  }

  private def tagsForLang(tags: Seq[Tag], lang: String) = {
    tags.find(_.language == lang).get.tags
  }
}
