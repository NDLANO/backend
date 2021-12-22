/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import cats.implicits._
import com.sksamuel.elastic4s.http.search.SearchHit
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties.{AudioControllerPath, Domain}
import no.ndla.audioapi.model.Language.{findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.audioapi.model.api.Title
import no.ndla.audioapi.model.domain.{AudioMetaInformation, SearchResult, SearchableTag}
import no.ndla.audioapi.model.search._
import no.ndla.audioapi.model.{Language, api, domain}
import no.ndla.audioapi.service.ConverterService

import scala.util.Try

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableSeries(s: domain.Series): Try[SearchableSeries] = {
      s.episodes
        .traverse(_.traverse(asSearchableAudioInformation))
        .map(searchableEpisodes => {
          SearchableSeries(
            id = s.id.toString,
            titles = SearchableLanguageValues.fromFields(s.title),
            descriptions = SearchableLanguageValues.fromFields(s.description),
            episodes = searchableEpisodes,
            coverPhoto = s.coverPhoto,
            lastUpdated = s.updated
          )
        })
    }

    def asAudioSummary(searchable: SearchableAudioInformation, language: String): Try[api.AudioSummary] = {
      val titles = searchable.titles.languageValues.map(lv => domain.Title(lv.value, lv.language))

      val domainPodcastMeta = searchable.podcastMetaIntroduction.languageValues.flatMap(lv => {
        searchable.podcastMeta
          .find(_.language == lv.language)
          .map(meta => {
            domain.PodcastMeta(
              introduction = lv.value,
              coverPhoto = meta.coverPhoto,
              language = lv.language
            )
          })
      })

      val title = findByLanguageOrBestEffort(titles, Some(language)) match {
        case None    => Title("", language)
        case Some(x) => Title(x.title, x.language)
      }

      val podcastMeta = findByLanguageOrBestEffort(domainPodcastMeta, Some(language))
        .map(converterService.toApiPodcastMeta)

      val manuscripts = searchable.manuscript.languageValues.map(lv => domain.Manuscript(lv.value, lv.language))
      val manuscript = findByLanguageOrBestEffort(manuscripts, Some(language)).map(converterService.toApiManuscript)

      val tags = searchable.tags.languageValues.map(lv => domain.Tag(lv.value, lv.language))
      val filePaths = searchable.filePaths.map(lv => domain.Title(lv.filePath, lv.language)) // Hacky but functional

      val supportedLanguages = getSupportedLanguages(titles, manuscripts, domainPodcastMeta, filePaths, tags)

      searchable.series
        .traverse(s => asSeriesSummary(s, language))
        .map(series =>
          api.AudioSummary(
            id = searchable.id.toLong,
            title = title,
            audioType = searchable.audioType,
            url = s"$Domain$AudioControllerPath${searchable.id}",
            license = searchable.license,
            supportedLanguages = supportedLanguages,
            podcastMeta = podcastMeta,
            manuscript = manuscript,
            series = series,
            lastUpdated = searchable.lastUpdated
        ))
    }

    def asSeriesSummary(searchable: SearchableSeries, language: String): Try[api.SeriesSummary] = {
      for {
        title <- converterService.findAndConvertDomainToApiField(
          searchable.titles.languageValues,
          Some(language),
          (lv: LanguageValue[String]) => api.Title(lv.value, lv.language))

        description <- converterService.findAndConvertDomainToApiField(
          searchable.descriptions.languageValues,
          Some(language),
          (lv: LanguageValue[String]) => api.Description(lv.value, lv.language))

        episodes <- searchable.episodes.traverse(eps =>
          eps.traverse(ep => searchConverterService.asAudioSummary(ep, language)))

        supportedLanguages = getSupportedLanguages(searchable.titles.languageValues,
                                                   searchable.descriptions.languageValues)
      } yield
        api.SeriesSummary(
          id = searchable.id.toLong,
          title = title,
          description = description,
          supportedLanguages = supportedLanguages,
          episodes = episodes,
          coverPhoto = converterService.toApiCoverPhoto(searchable.coverPhoto)
        )
    }

    def asSearchableAudioInformation(ai: AudioMetaInformation): Try[SearchableAudioInformation] = {
      val metaWithAgreement = converterService.withAgreementCopyright(ai)

      val defaultTitle = metaWithAgreement.titles
        .sortBy(title => {
          val languagePriority = Language.languageAnalyzers.map(la => la.languageTag.toString()).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      val authors =
        metaWithAgreement.copyright.creators.map(_.name) ++
          metaWithAgreement.copyright.processors.map(_.name) ++
          metaWithAgreement.copyright.rightsholders.map(_.name)

      val podcastMetaIntros = SearchableLanguageValues(
        metaWithAgreement.podcastMeta.map(pm => LanguageValue(pm.language, pm.introduction)))

      val searchablePodcastMeta = metaWithAgreement.podcastMeta.map(pm =>
        SearchablePodcastMeta(coverPhoto = pm.coverPhoto, language = pm.language))

      val searchableAudios = metaWithAgreement.filePaths.map(fp => SearchableAudio(fp.filePath, fp.language))

      metaWithAgreement.series
        .traverse(s => asSearchableSeries(s))
        .map(series =>
          SearchableAudioInformation(
            id = metaWithAgreement.id.get.toString,
            titles = SearchableLanguageValues.fromFields(metaWithAgreement.titles),
            tags = SearchableLanguageList.fromFields(metaWithAgreement.tags),
            filePaths = searchableAudios,
            license = metaWithAgreement.copyright.license,
            authors = authors,
            lastUpdated = metaWithAgreement.updated,
            defaultTitle = defaultTitle.map(t => t.title),
            audioType = metaWithAgreement.audioType.toString,
            podcastMetaIntroduction = podcastMetaIntros,
            podcastMeta = searchablePodcastMeta,
            manuscript = SearchableLanguageValues.fromFields(metaWithAgreement.manuscript),
            series = series
        ))
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
            Language.languageAnalyzers.map(la => la.languageTag.toString()).reverse.indexOf(lang)
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

    def asApiAudioSummarySearchResult(
        searchResult: domain.SearchResult[api.AudioSummary]): api.AudioSummarySearchResult =
      api.AudioSummarySearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )

    def asApiSeriesSummarySearchResult(
        searchResult: domain.SearchResult[api.SeriesSummary]): api.SeriesSummarySearchResult =
      api.SeriesSummarySearchResult(
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
