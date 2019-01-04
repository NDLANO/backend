/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.http.search.SearchHit
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.model.Language
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.api
import no.ndla.audioapi.model.search.{
  LanguageValue,
  SearchableAudioInformation,
  SearchableLanguageList,
  SearchableLanguageValues
}
import no.ndla.audioapi.service.ConverterService
import no.ndla.mapping.ISO639
import no.ndla.network.ApplicationUrl

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableAudioInformation(ai: AudioMetaInformation): SearchableAudioInformation = {
      val metaWithAgreement = converterService.withAgreementCopyright(ai)

      val defaultTitle = metaWithAgreement.titles
        .sortBy(title => {
          val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      SearchableAudioInformation(
        id = metaWithAgreement.id.get.toString,
        titles =
          SearchableLanguageValues(metaWithAgreement.titles.map(title => LanguageValue(title.language, title.title))),
        tags = SearchableLanguageList(metaWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        license = metaWithAgreement.copyright.license,
        authors = metaWithAgreement.copyright.creators.map(_.name) ++ metaWithAgreement.copyright.processors
          .map(_.name) ++ metaWithAgreement.copyright.rightsholders.map(_.name),
        lastUpdated = metaWithAgreement.updated,
        defaultTitle = defaultTitle.map(t => t.title)
      )
    }

    def createUrlToAudio(id: String): String = {
      s"${ApplicationUrl.get}$id"
    }

    def getLanguageFromHit(result: SearchHit): Option[String] = {
      def keyToLanguage(keys: Iterable[String]): Option[String] = {
        val keyLanguages = keys.toList.flatMap(key =>
          key.split('.').toList match {
            case _ :: language :: _ => Some(language)
            case _                  => None
        })

        keyLanguages
          .sortBy(lang => {
            ISO639.languagePriority.reverse.indexOf(lang)
          })
          .lastOption
      }

      val highlightKeys: Option[Map[String, _]] = Option(result.highlight)
      val matchLanguage = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          keyToLanguage(result.sourceAsMap.keys)
      }
    }

    def asApiSearchResult(searchResult: domain.SearchResult): api.SearchResult =
      api.SearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )
  }
}
