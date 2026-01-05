/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import com.sksamuel.elastic4s.requests.searches.SearchHit
import no.ndla.common.model.api.search.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.common.model.domain.learningpath.{LearningPath, LearningStep, StepType}
import no.ndla.language.Language.{
  findByLanguageOrBestEffort,
  getDefault,
  getSupportedLanguages,
  sortLanguagesByPriority,
}
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.*
import no.ndla.learningpathapi.model.api.{LearningPathSummaryV2DTO, SearchResultV2DTO}
import no.ndla.learningpathapi.model.domain.*
import no.ndla.learningpathapi.model.search.*
import no.ndla.learningpathapi.service.ConverterService
import no.ndla.network.ApplicationUrl

class SearchConverterServiceComponent(using converterService: ConverterService, props: Props) {
  def asApiLearningPathSummaryV2(
      searchableLearningPath: SearchableLearningPath,
      language: String,
  ): LearningPathSummaryV2DTO = {
    val titles       = searchableLearningPath.titles.languageValues.map(lv => api.TitleDTO(lv.value, lv.language))
    val descriptions = searchableLearningPath
      .descriptions
      .languageValues
      .map(lv => api.DescriptionDTO(lv.value, lv.language))
    val introductions = searchableLearningPath.introductions.languageValues.size match {
      case 0 => searchableLearningPath
          .learningsteps
          .find(_.stepType == StepType.INTRODUCTION.toString)
          .map(step => step.descriptions.languageValues.map(lv => api.IntroductionDTO(lv.value, lv.language)))
          .getOrElse(Seq.empty)
      case _ =>
        searchableLearningPath.introductions.languageValues.map(lv => api.IntroductionDTO(lv.value, lv.language))
    }
    val tags               = searchableLearningPath.tags.languageValues.map(lv => api.LearningPathTagsDTO(lv.value, lv.language))
    val supportedLanguages = getSupportedLanguages(titles, descriptions, introductions, tags)

    LearningPathSummaryV2DTO(
      searchableLearningPath.id,
      revision = None,
      findByLanguageOrBestEffort(titles, language).getOrElse(api.TitleDTO("", props.DefaultLanguage)),
      findByLanguageOrBestEffort(descriptions, language).getOrElse(api.DescriptionDTO("", props.DefaultLanguage)),
      findByLanguageOrBestEffort(introductions, language).getOrElse(api.IntroductionDTO("", props.DefaultLanguage)),
      createUrlToLearningPath(searchableLearningPath.id),
      searchableLearningPath.coverPhotoUrl,
      searchableLearningPath.duration,
      searchableLearningPath.status,
      searchableLearningPath.created,
      searchableLearningPath.lastUpdated,
      findByLanguageOrBestEffort(tags, language).getOrElse(api.LearningPathTagsDTO(Seq(), props.DefaultLanguage)),
      searchableLearningPath.copyright,
      supportedLanguages,
      searchableLearningPath.isBasedOn,
      message = None,
      grepCodes = searchableLearningPath.grepCodes,
    )
  }

  def asSearchableLearningpath(learningPath: LearningPath): SearchableLearningPath = {
    val defaultTitle = getDefault(learningPath.title)

    SearchableLearningPath(
      id = learningPath.id.get,
      titles = SearchableLanguageValues(learningPath.title.map(title => LanguageValue(title.language, title.title))),
      descriptions =
        SearchableLanguageValues(learningPath.description.map(desc => LanguageValue(desc.language, desc.description))),
      SearchableLanguageValues(
        learningPath.introduction.map(intro => LanguageValue(intro.language, intro.introduction))
      ),
      learningPath.coverPhotoId.flatMap(converterService.asCoverPhoto).map(_.url),
      learningPath.duration,
      learningPath.status.toString,
      learningPath.verificationStatus.toString,
      learningPath.created,
      learningPath.lastUpdated,
      defaultTitle.map(_.title),
      SearchableLanguageList(learningPath.tags.map(tags => LanguageValue(tags.language, tags.tags))),
      learningPath.withOnlyActiveSteps.learningsteps.map(asSearchableLearningStep).toList,
      converterService.asApiCopyright(learningPath.copyright),
      learningPath.isBasedOn,
      learningPath.grepCodes,
    )
  }

  private def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
    SearchableLearningStep(
      learningStep.`type`.toString,
      learningStep.embedUrl.map(_.url).toList,
      learningStep.articleId,
      learningStep.status.entryName,
      SearchableLanguageValues(learningStep.title.map(title => LanguageValue(title.language, title.title))),
      SearchableLanguageValues(learningStep.description.map(desc => LanguageValue(desc.language, desc.description))),
    )
  }

  def createUrlToLearningPath(id: Long): String = {
    s"${ApplicationUrl.get}$id"
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

  def asApiSearchResult(searchResult: SearchResult): SearchResultV2DTO = SearchResultV2DTO(
    searchResult.totalCount,
    searchResult.page,
    searchResult.pageSize,
    searchResult.language,
    searchResult.results,
  )

}
