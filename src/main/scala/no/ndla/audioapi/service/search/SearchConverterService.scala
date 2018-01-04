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
import no.ndla.audioapi.model.search.{LanguageValue, SearchableAudioInformation, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.audioapi.service.ConverterService
import no.ndla.network.ApplicationUrl

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableAudioInformation(ai: AudioMetaInformation): SearchableAudioInformation = {
      val metaWithAgreement = converterService.withAgreementCopyright(ai)

      val defaultTitle = metaWithAgreement.titles.sortBy(title => {
        val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
        languagePriority.indexOf(title.language)
      }).lastOption

      SearchableAudioInformation(
        id = metaWithAgreement.id.get.toString,
        titles = SearchableLanguageValues(metaWithAgreement.titles.map(title => LanguageValue(title.language, title.title))),
        tags = SearchableLanguageList(metaWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        license = metaWithAgreement.copyright.license,
        authors = metaWithAgreement.copyright.creators.map(_.name) ++ metaWithAgreement.copyright.processors.map(_.name) ++ metaWithAgreement.copyright.rightsholders.map(_.name),
        lastUpdated = metaWithAgreement.updated,
        defaultTitle = defaultTitle.map(t => t.title)
      )
    }

    def createUrlToAudio(id: String): String = {
      s"${ApplicationUrl.get}$id"
    }

    def getLanguageFromHit(result: SearchHit): Option[String] = {
      val sortedInnerHits = result.innerHits.toList.filter(ih => ih._2.total > 0).sortBy{
        case (_, hit) => hit.max_score
      }.reverse

      val matchLanguage = sortedInnerHits.headOption.flatMap{
        case (_, innerHit) =>
          innerHit.hits.sortBy(hit => hit.score).reverse.headOption.flatMap(hit => {
            hit.highlight.headOption.map(hl => {
              hl._1.split('.').filterNot(_ == "raw").last
            })
          })
      }

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          val title = result.sourceAsMap.get("titles")
          val titleMap = title.map(tm => {
            tm.asInstanceOf[Map[String, _]]
          })

          val languages = titleMap.map(title => title.keySet.toList)

          languages.flatMap(languageList => {
            languageList.sortBy(lang => {
              val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
              languagePriority.indexOf(lang)
            }).lastOption
          })
      }
    }
  }
}
