/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.model.api.{AudioSummary, Title}
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.search.{LanguageValue, SearchableAudioInformation, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.network.ApplicationUrl

trait SearchConverterService {
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableAudioInformation(ai: AudioMetaInformation): SearchableAudioInformation = {
      SearchableAudioInformation(
        id = ai.id.get.toString,
        titles = SearchableLanguageValues(ai.titles.map(title => LanguageValue(title.language, title.title))),
        tags = SearchableLanguageList(ai.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        license = ai.copyright.license,
        authors = ai.copyright.authors.map(_.name))
    }

    def asAudioSummary(searchableArticleInformation: SearchableAudioInformation): AudioSummary = {
      AudioSummary(
        id = searchableArticleInformation.id.toLong,
        titles = searchableArticleInformation.titles.languageValues.map(lv => Title(lv.value, lv.lang)),
        url = createUrlToAudio(searchableArticleInformation.id),
        license = searchableArticleInformation.license)
    }

    def createUrlToAudio(id: String): String = {
      s"${ApplicationUrl.get}$id"
    }
  }
}
