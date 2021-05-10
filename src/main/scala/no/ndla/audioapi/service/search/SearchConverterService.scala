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
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.AudioApiProperties.{AudioControllerPath, Domain}
import no.ndla.audioapi.model.Language
import no.ndla.audioapi.model.Language.{findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.audioapi.model.domain.{AudioMetaInformation, LanguageField, SearchResult, SearchableTag, WithLanguage}
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.api
import no.ndla.audioapi.model.api.{AudioSummary, ElasticIndexingException, MissingIdException, Title}
import no.ndla.audioapi.model.search.{
  LanguageValue,
  SearchableAudioInformation,
  SearchableLanguageList,
  SearchableLanguageValues,
  SearchableSeries
}
import no.ndla.audioapi.service.ConverterService
import no.ndla.mapping.ISO639
import no.ndla.network.ApplicationUrl
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods

import scala.util.{Failure, Success, Try}

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableSeries(s: domain.Series): Try[SearchableSeries] = {
      s.episodes match {
        case None =>
          Failure(MissingIdException(s"Series without episodes was passed to `asSearchableSeries`, this is a bug."))
        case Some(episodes) =>
          Success(
            SearchableSeries(
              id = s.id.toString,
              titles = SearchableLanguageValues(s.title.map(t => LanguageValue(t.language, t.title))),
              episodes = episodes.map(asSearchableAudioInformation),
              coverPhoto = s.coverPhoto
            )
          )
      }
    }

    def asAudioSummary(searchable: SearchableAudioInformation, language: String): Try[api.AudioSummary] = {
      val titles = searchable.titles.languageValues.map(lv => domain.Title(lv.value, lv.language))
      val supportedLanguages = getSupportedLanguages(titles, searchable.podcastMeta)
      val title = findByLanguageOrBestEffort(titles, Some(language)) match {
        case None    => Title("", language)
        case Some(x) => Title(x.title, x.language)
      }

      val podcastMeta = findByLanguageOrBestEffort(searchable.podcastMeta, Some(language))
        .map(converterService.toApiPodcastMeta)

      Success(
        api.AudioSummary(
          id = searchable.id.toLong,
          title = title,
          audioType = searchable.audioType,
          url = s"$Domain$AudioControllerPath${searchable.id}",
          license = searchable.license,
          supportedLanguages = supportedLanguages,
          podcastMeta = podcastMeta
        )
      )

    }

    def asSearchableAudioInformation(ai: AudioMetaInformation): SearchableAudioInformation = {
      val metaWithAgreement = converterService.withAgreementCopyright(ai)

      val defaultTitle = metaWithAgreement.titles
        .sortBy(title => {
          val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      val authors =
        metaWithAgreement.copyright.creators.map(_.name) ++
          metaWithAgreement.copyright.processors.map(_.name) ++
          metaWithAgreement.copyright.rightsholders.map(_.name)

      val titles =
        SearchableLanguageValues(metaWithAgreement.titles.map(title => LanguageValue(title.language, title.title)))

      SearchableAudioInformation(
        id = metaWithAgreement.id.get.toString,
        titles = titles,
        tags = SearchableLanguageList(metaWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        license = metaWithAgreement.copyright.license,
        authors = authors,
        lastUpdated = metaWithAgreement.updated,
        defaultTitle = defaultTitle.map(t => t.title),
        audioType = metaWithAgreement.audioType.toString,
        podcastMeta = metaWithAgreement.podcastMeta
      )
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

    def asApiSearchResult[T](searchResult: domain.SearchResult[T]): api.SearchResult[T] =
      api.SearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )

    def asSearchableTags(audio: domain.AudioMetaInformation): Seq[SearchableTag] =
      audio.tags.flatMap(
        audioTags =>
          audioTags.tags.map(
            tag =>
              SearchableTag(
                tag = tag,
                language = audioTags.language
            )))

    def tagSearchResultAsApiResult(searchResult: SearchResult[String]): api.TagsSearchResult =
      api.TagsSearchResult(
        searchResult.totalCount,
        searchResult.page.getOrElse(1),
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )
  }
}
