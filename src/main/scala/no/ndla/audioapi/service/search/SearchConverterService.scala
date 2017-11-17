/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.search.{LanguageValue, SearchableAudioInformation, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.audioapi.service.ConverterService
import no.ndla.network.ApplicationUrl

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableAudioInformation(ai: AudioMetaInformation): SearchableAudioInformation = {
      val metaWithAgreement = converterService.withAgreementCopyright(ai)

      SearchableAudioInformation(
        id = metaWithAgreement.id.get.toString,
        titles = SearchableLanguageValues(metaWithAgreement.titles.map(title => LanguageValue(title.language, title.title))),
        tags = SearchableLanguageList(metaWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        license = metaWithAgreement.copyright.license,
        authors = metaWithAgreement.copyright.creators.map(_.name) ++ metaWithAgreement.copyright.processors.map(_.name) ++ metaWithAgreement.copyright.rightsholders.map(_.name)
      )
    }

    def createUrlToAudio(id: String): String = {
      s"${ApplicationUrl.get}$id"
    }
  }
}
