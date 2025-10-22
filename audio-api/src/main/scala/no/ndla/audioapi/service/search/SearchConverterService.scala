/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.TitleDTO
import no.ndla.audioapi.model.domain.{AudioMetaInformation, SearchResult, SearchableTag}
import no.ndla.audioapi.model.search.*
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.service.ConverterService
import no.ndla.common.model.api.search.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.common.model.domain as common
import no.ndla.language.Language.{
  findByLanguageOrBestEffort,
  getDefault,
  getSupportedLanguages,
  sortLanguagesByPriority,
}

import scala.util.Try

class SearchConverterService(using converterService: ConverterService, props: Props) extends StrictLogging {

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
          lastUpdated = s.updated,
        )
      })
  }

  def asAudioSummary(searchable: SearchableAudioInformation, language: String): Try[api.AudioSummaryDTO] = {
    val titles = searchable.titles.languageValues.map(lv => common.Title(lv.value, lv.language))

    val domainPodcastMeta = searchable
      .podcastMetaIntroduction
      .languageValues
      .flatMap(lv => {
        searchable
          .podcastMeta
          .find(_.language == lv.language)
          .map(meta => {
            domain.PodcastMeta(introduction = lv.value, coverPhoto = meta.coverPhoto, language = lv.language)
          })
      })

    val title = findByLanguageOrBestEffort(titles, language) match {
      case None    => TitleDTO("", language)
      case Some(x) => TitleDTO(x.title, x.language)
    }

    val podcastMeta = findByLanguageOrBestEffort(domainPodcastMeta, language).map(converterService.toApiPodcastMeta)

    val manuscripts = searchable.manuscript.languageValues.map(lv => domain.Manuscript(lv.value, lv.language))
    val manuscript  = findByLanguageOrBestEffort(manuscripts, language).map(converterService.toApiManuscript)

    val tags      = searchable.tags.languageValues.map(lv => common.Tag(lv.value, lv.language))
    val filePaths = searchable.filePaths.map(lv => common.Title(lv.filePath, lv.language)) // Hacky but functional

    val supportedLanguages = getSupportedLanguages(titles, manuscripts, domainPodcastMeta, filePaths, tags)

    searchable
      .series
      .traverse(s => asSeriesSummary(s, language))
      .map(series =>
        api.AudioSummaryDTO(
          id = searchable.id.toLong,
          title = title,
          audioType = searchable.audioType,
          url = s"${props.Domain}${props.AudioControllerPath}${searchable.id}",
          license = searchable.license,
          supportedLanguages = supportedLanguages,
          podcastMeta = podcastMeta,
          manuscript = manuscript,
          series = series,
          lastUpdated = searchable.lastUpdated,
        )
      )
  }

  def asSeriesSummary(searchable: SearchableSeries, language: String): Try[api.SeriesSummaryDTO] = {
    for {
      title <- converterService
        .findAndConvertDomainToApiField(searchable.titles.languageValues, Some(language))
        .map(lv => api.TitleDTO(lv.value, lv.language))

      description <- converterService
        .findAndConvertDomainToApiField(searchable.descriptions.languageValues, Some(language))
        .map(lv => api.DescriptionDTO(lv.value, lv.language))

      episodes <- searchable.episodes.traverse(eps => eps.traverse(ep => asAudioSummary(ep, language)))

      supportedLanguages =
        getSupportedLanguages(searchable.titles.languageValues, searchable.descriptions.languageValues)
    } yield api.SeriesSummaryDTO(
      id = searchable.id.toLong,
      title = title,
      description = description,
      supportedLanguages = supportedLanguages,
      episodes = episodes,
      coverPhoto = converterService.toApiCoverPhoto(searchable.coverPhoto),
    )
  }

  def asSearchableAudioInformation(ai: AudioMetaInformation): Try[SearchableAudioInformation] = {
    val defaultTitle = getDefault(ai.titles)

    val authors = ai.copyright.creators.map(_.name) ++
      ai.copyright.processors.map(_.name) ++
      ai.copyright.rightsholders.map(_.name)

    val podcastMetaIntros =
      SearchableLanguageValues(ai.podcastMeta.map(pm => LanguageValue(pm.language, pm.introduction)))

    val searchablePodcastMeta = ai
      .podcastMeta
      .map(pm => SearchablePodcastMeta(coverPhoto = pm.coverPhoto, language = pm.language))

    val searchableAudios = ai.filePaths.map(fp => SearchableAudio(fp.filePath, fp.language))

    ai.series
      .traverse(s => asSearchableSeries(s))
      .map(series =>
        SearchableAudioInformation(
          id = ai.id.get.toString,
          titles = SearchableLanguageValues.fromFields(ai.titles),
          tags = SearchableLanguageList.fromFields(ai.tags),
          filePaths = searchableAudios,
          license = ai.copyright.license,
          authors = authors,
          lastUpdated = ai.updated,
          defaultTitle = defaultTitle.map(t => t.title),
          audioType = ai.audioType.toString,
          podcastMetaIntroduction = podcastMetaIntros,
          podcastMeta = searchablePodcastMeta,
          manuscript = SearchableLanguageValues.fromFields(ai.manuscript),
          series = series,
        )
      )
  }

  def getLanguageFromHit(result: SearchHit): Option[String] = {
    def keyToLanguage(keys: Iterable[String]): Option[String] = {
      val keyLanguages = keys
        .toList
        .flatMap(key =>
          key.split('.').toList match {
            case _ :: language :: _ => Some(language)
            case _                  => None
          }
        )

      sortLanguagesByPriority(keyLanguages).headOption
    }

    val highlightKeys: Option[Map[String, ?]] = Option(result.highlight)
    val matchLanguage                         = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

    matchLanguage match {
      case Some(lang) => Some(lang)
      case _          => keyToLanguage(result.sourceAsMap.keys)
    }
  }

  def asApiAudioSummarySearchResult(
      searchResult: domain.SearchResult[api.AudioSummaryDTO]
  ): api.AudioSummarySearchResultDTO = api.AudioSummarySearchResultDTO(
    searchResult.totalCount,
    searchResult.page,
    searchResult.pageSize,
    searchResult.language,
    searchResult.results,
  )

  def asApiSeriesSummarySearchResult(
      searchResult: domain.SearchResult[api.SeriesSummaryDTO]
  ): api.SeriesSummarySearchResultDTO = api.SeriesSummarySearchResultDTO(
    searchResult.totalCount,
    searchResult.page,
    searchResult.pageSize,
    searchResult.language,
    searchResult.results,
  )

  def asSearchableTags(audio: domain.AudioMetaInformation): Seq[SearchableTag] = audio
    .tags
    .flatMap(audioTags => audioTags.tags.map(tag => SearchableTag(tag = tag, language = audioTags.language)))

  def tagSearchResultAsApiResult(searchResult: SearchResult[String]): api.TagsSearchResultDTO = api.TagsSearchResultDTO(
    searchResult.totalCount,
    searchResult.page.getOrElse(1),
    searchResult.pageSize,
    searchResult.language,
    searchResult.results,
  )
}
