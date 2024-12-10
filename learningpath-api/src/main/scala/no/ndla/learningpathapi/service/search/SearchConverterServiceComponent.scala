/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import com.sksamuel.elastic4s.requests.searches.SearchHit
import no.ndla.common.model.domain.learningpath.{LearningPath, LearningStep, StepType}
import no.ndla.language.Language.{findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.*
import no.ndla.learningpathapi.model.api.{LearningPathSummaryV2DTO, SearchResultV2DTO}
import no.ndla.learningpathapi.model.domain.*
import no.ndla.learningpathapi.model.search.*
import no.ndla.learningpathapi.service.ConverterService
import no.ndla.mapping.ISO639
import no.ndla.network.ApplicationUrl
import no.ndla.search.SearchLanguage
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}

trait SearchConverterServiceComponent {
  this: ConverterService & Props =>
  val searchConverterService: SearchConverterService

  class SearchConverterService {
    import props.DefaultLanguage

    def asApiLearningPathSummaryV2(
        searchableLearningPath: SearchableLearningPath,
        language: String
    ): LearningPathSummaryV2DTO = {
      val titles = searchableLearningPath.titles.languageValues.map(lv => api.TitleDTO(lv.value, lv.language))
      val descriptions =
        searchableLearningPath.descriptions.languageValues.map(lv => api.DescriptionDTO(lv.value, lv.language))
      val introductions =
        searchableLearningPath.learningsteps.find(_.stepType == StepType.INTRODUCTION.toString) match {
          case Some(step) => step.descriptions.languageValues.map(lv => api.IntroductionDTO(lv.value, lv.language))
          case _          => Seq.empty
        }
      val tags = searchableLearningPath.tags.languageValues.map(lv => api.LearningPathTagsDTO(lv.value, lv.language))
      val supportedLanguages = getSupportedLanguages(titles, descriptions, introductions, tags)

      LearningPathSummaryV2DTO(
        searchableLearningPath.id,
        revision = None,
        findByLanguageOrBestEffort(titles, language)
          .getOrElse(api.TitleDTO("", DefaultLanguage)),
        findByLanguageOrBestEffort(descriptions, language)
          .getOrElse(api.DescriptionDTO("", DefaultLanguage)),
        findByLanguageOrBestEffort(introductions, language)
          .getOrElse(api.IntroductionDTO("", DefaultLanguage)),
        createUrlToLearningPath(searchableLearningPath.id),
        searchableLearningPath.coverPhotoUrl,
        searchableLearningPath.duration,
        searchableLearningPath.status,
        searchableLearningPath.created,
        searchableLearningPath.lastUpdated,
        findByLanguageOrBestEffort(tags, language)
          .getOrElse(api.LearningPathTagsDTO(Seq(), DefaultLanguage)),
        searchableLearningPath.copyright,
        supportedLanguages,
        searchableLearningPath.isBasedOn,
        message = None
      )
    }

    def asSearchableLearningpath(learningPath: LearningPath): SearchableLearningPath = {
      val defaultTitle = learningPath.title
        .sortBy(title => {
          val languagePriority =
            SearchLanguage.languageAnalyzers.map(la => la.languageTag.toString).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      SearchableLearningPath(
        learningPath.id.get,
        SearchableLanguageValues(learningPath.title.map(title => LanguageValue(title.language, title.title))),
        SearchableLanguageValues(learningPath.description.map(desc => LanguageValue(desc.language, desc.description))),
        learningPath.coverPhotoId
          .flatMap(converterService.asCoverPhoto)
          .map(_.url),
        learningPath.duration,
        learningPath.status.toString,
        learningPath.verificationStatus.toString,
        learningPath.created,
        learningPath.lastUpdated,
        defaultTitle.map(_.title),
        SearchableLanguageList(learningPath.tags.map(tags => LanguageValue(tags.language, tags.tags))),
        learningPath.learningsteps.getOrElse(Seq.empty).map(asSearchableLearningStep).toList,
        converterService.asApiCopyright(learningPath.copyright),
        learningPath.isBasedOn
      )
    }

    private def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      SearchableLearningStep(
        learningStep.`type`.toString,
        learningStep.embedUrl.map(_.url).toList,
        learningStep.status.entryName,
        SearchableLanguageValues(learningStep.title.map(title => LanguageValue(title.language, title.title))),
        SearchableLanguageValues(learningStep.description.map(desc => LanguageValue(desc.language, desc.description)))
      )
    }

    def createUrlToLearningPath(id: Long): String = {
      s"${ApplicationUrl.get}$id"
    }

    def getLanguageFromHit(result: SearchHit): Option[String] = {
      def keyToLanguage(keys: Iterable[String]): Option[String] = {
        val keyLanguages = keys.toList.flatMap(key =>
          key.split('.').toList match {
            case _ :: language :: _ => Some(language)
            case _                  => None
          }
        )

        keyLanguages
          .sortBy(lang => {
            ISO639.languagePriority.reverse.indexOf(lang)
          })
          .lastOption
      }

      val highlightKeys: Option[Map[String, ?]] = Option(result.highlight)
      val matchLanguage                         = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          keyToLanguage(result.sourceAsMap.keys)
      }
    }

    def asApiSearchResult(searchResult: SearchResult): SearchResultV2DTO =
      SearchResultV2DTO(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )

  }

}
