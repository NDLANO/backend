/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import no.ndla.audioapi.model.api
import no.ndla.audioapi.model.domain.{AudioMetaInformation, _}
import no.ndla.audioapi.model.search.{SearchableAudioInformation, SearchableLanguageList, SearchableLanguageValues}

class SearchConverterServiceTest extends UnitSuite with TestEnvironment {

  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright("by-nc-sa", Some("Gotham City"), List(Author("Forfatter", "DC Comics")))

  val domainTitles = List(
    Title("Bokmål tittel", Some("nb")), Title("Nynorsk tittel", Some("nn")),
    Title("English title", Some("en")), Title("Titre francais", Some("fr")),
    Title("Deutsch titel", Some("de")), Title("Titulo espanol", Some("es")),
    Title("Nekonata titolo", None))

  val apiTitles = List(
    api.Title("Bokmål tittel", Some("nb")), api.Title("Nynorsk tittel", Some("nn")),
    api.Title("English title", Some("en")), api.Title("Titre francais", Some("fr")),
    api.Title("Deutsch titel", Some("de")), api.Title("Titulo espanol", Some("es")),
    api.Title("Nekonata titolo", None))

  val audioFiles = Seq(
    Audio("file.mp3", "audio/mpeg", 1024, Some("nb")),
    Audio("file2.mp3", "audio/mpeg", 2048, Some("nb")),
    Audio("file3.mp3", "audio/mpeg", 4096, Some("nb")),
    Audio("file4.mp3", "audio/mpeg", 8192, Some("nb"))
  )

  val audioTags = Seq(
    Tag(Seq("fugl", "fisk"), Some("nb")), Tag(Seq("fugl", "fisk"), Some("nn")),
    Tag(Seq("bird", "fish"), Some("en")), Tag(Seq("got", "tired"), Some("fr")),
    Tag(Seq("of", "translating"), Some("de")), Tag(Seq("all", "of"), Some("es")),
    Tag(Seq("the", "words"), None)
  )

  test("That asSearchableAudioInformation converts titles with correct language") {
    val audio = AudioMetaInformation(Some(1), domainTitles, audioFiles, byNcSa, audioTags)
    val searchableAudio = searchConverterService.asSearchableAudioInformation(audio)
    verifyTitles(searchableAudio)
  }


  test("That asSearchableAudioInformation converts articles with correct language") {
    val audio = AudioMetaInformation(Some(1), domainTitles, audioFiles, byNcSa, audioTags)
    val searchableAudio = searchConverterService.asSearchableAudioInformation(audio)
  }


  test("That asSearchableAudioInformation converts tags with correct language") {
    val audio = AudioMetaInformation(Some(1), domainTitles, audioFiles, byNcSa, audioTags)
    val searchableAudio = searchConverterService.asSearchableAudioInformation(audio)
    verifyTags(searchableAudio)
  }

  test("That asSearchableAudioInformation converts all fields with correct language") {
    val audio = AudioMetaInformation(Some(1), domainTitles, audioFiles, byNcSa, audioTags)
    val searchableAudio = searchConverterService.asSearchableAudioInformation(audio)

    verifyTitles(searchableAudio)
    verifyTags(searchableAudio)
  }

  test("That asAudioSummary converts all fields with correct language") {
    val audio = AudioMetaInformation(Some(1), domainTitles, audioFiles, byNcSa, audioTags)
    val searchableAudio = searchConverterService.asSearchableAudioInformation(audio)
    val articleSummary = searchConverterService.asAudioSummary(searchableAudio)

    articleSummary.id should equal (audio.id.get)
    articleSummary.license should equal (audio.copyright.license)
    articleSummary.titles should equal (apiTitles)
  }

  private def verifyTitles(searchableAudio: SearchableAudioInformation): Unit = {
    searchableAudio.titles.languageValues.size should equal(domainTitles.size)
    languageValueWithLang(searchableAudio.titles, "nb") should equal(titleForLang(domainTitles, "nb"))
    languageValueWithLang(searchableAudio.titles, "nn") should equal(titleForLang(domainTitles, "nn"))
    languageValueWithLang(searchableAudio.titles, "en") should equal(titleForLang(domainTitles, "en"))
    languageValueWithLang(searchableAudio.titles, "fr") should equal(titleForLang(domainTitles, "fr"))
    languageValueWithLang(searchableAudio.titles, "de") should equal(titleForLang(domainTitles, "de"))
    languageValueWithLang(searchableAudio.titles, "es") should equal(titleForLang(domainTitles, "es"))
    searchableAudio.titles.languageValues.find(_.lang.isEmpty).get.value should equal(domainTitles.find(_.language.isEmpty).get.title)
  }

  private def verifyTags(searchableAudio: SearchableAudioInformation): Unit = {
    languageListWithLang(searchableAudio.tags, "nb") should equal(tagsForLang(audioTags, "nb"))
    languageListWithLang(searchableAudio.tags, "nn") should equal(tagsForLang(audioTags, "nn"))
    languageListWithLang(searchableAudio.tags, "en") should equal(tagsForLang(audioTags, "en"))
    languageListWithLang(searchableAudio.tags, "fr") should equal(tagsForLang(audioTags, "fr"))
    languageListWithLang(searchableAudio.tags, "de") should equal(tagsForLang(audioTags, "de"))
    languageListWithLang(searchableAudio.tags, "es") should equal(tagsForLang(audioTags, "es"))
    languageListWithLang(searchableAudio.tags, null) should equal(tagsForLang(audioTags))
  }

  private def languageValueWithLang(languageValues: SearchableLanguageValues, lang: String = null): String = {
    languageValues.languageValues.find(_.lang == Option(lang)).get.value
  }

  private def languageListWithLang(languageList: SearchableLanguageList, lang: String = null): Seq[String] = {
    languageList.languageValues.find(_.lang == Option(lang)).get.value
  }

  private def titleForLang(titles: Seq[Title], lang: String = null): String = {
    titles.find(_.language == Option(lang)).get.title
  }

  private def tagsForLang(tags: Seq[Tag], lang: String = null) = {
    tags.find(_.language == Option(lang)).get.tags
  }
}
