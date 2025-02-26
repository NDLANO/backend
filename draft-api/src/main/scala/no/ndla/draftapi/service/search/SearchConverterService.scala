/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain as common
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api.ArticleSearchResultDTO
import no.ndla.draftapi.model.domain.SearchResult
import no.ndla.draftapi.model.search.*
import no.ndla.draftapi.service.ConverterService
import no.ndla.language.Language.{UnknownLanguage, findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.mapping.ISO639
import no.ndla.network.ApplicationUrl
import no.ndla.search.SearchLanguage
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import org.jsoup.Jsoup

trait SearchConverterService {
  this: ConverterService & SearchLanguage =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends StrictLogging {
    def asSearchableArticle(ai: Draft): SearchableArticle = {

      val defaultTitle = ai.title
        .sortBy(title => {
          val languagePriority = SearchLanguage.languageAnalyzers.map(la => la.languageTag.toString).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      SearchableArticle(
        id = ai.id.get,
        title = SearchableLanguageValues(ai.title.map(title => LanguageValue(title.language, title.title))),
        visualElement =
          SearchableLanguageValues(ai.visualElement.map(visual => LanguageValue(visual.language, visual.resource))),
        introduction =
          SearchableLanguageValues(ai.introduction.map(intro => LanguageValue(intro.language, intro.introduction))),
        content = SearchableLanguageValues(
          ai.content.map(article => LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))
        ),
        tags = SearchableLanguageList(ai.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = ai.updated,
        license = ai.copyright.flatMap(_.license),
        authors = ai.copyright
          .map(copy => copy.creators ++ copy.processors ++ copy.rightsholders)
          .map(a => a.map(_.name))
          .toSeq
          .flatten,
        articleType = ai.articleType.entryName,
        notes = ai.notes.map(_.note),
        defaultTitle = defaultTitle.map(_.title),
        users = ai.updatedBy +: ai.notes.map(_.user),
        previousNotes = ai.previousVersionsNotes.map(_.note),
        grepCodes = ai.grepCodes,
        status = SearchableStatus(ai.status.current, ai.status.other)
      )
    }

    def hitAsArticleSummary(hitString: String, language: String): api.ArticleSummaryDTO = {
      val searchableArticle = CirceUtil.unsafeParseAs[SearchableArticle](hitString)

      val titles = searchableArticle.title.languageValues.map(lv => common.Title(lv.value, lv.language))
      val introductions =
        searchableArticle.introduction.languageValues.map(lv => common.Introduction(lv.value, lv.language))
      val visualElements =
        searchableArticle.visualElement.languageValues.map(lv => common.VisualElement(lv.value, lv.language))
      val tags  = searchableArticle.tags.languageValues.map(lv => common.Tag(lv.value, lv.language))
      val notes = searchableArticle.notes
      val users = searchableArticle.users

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions)

      val title = findByLanguageOrBestEffort(titles, language)
        .map(converterService.toApiArticleTitle)
        .getOrElse(api.ArticleTitleDTO("", "", UnknownLanguage.toString))
      val visualElement = findByLanguageOrBestEffort(visualElements, language).map(converterService.toApiVisualElement)
      val introduction =
        findByLanguageOrBestEffort(introductions, language).map(converterService.toApiArticleIntroduction)
      val tag = findByLanguageOrBestEffort(tags, language).map(converterService.toApiArticleTag)
      val status =
        api.StatusDTO(searchableArticle.status.current.toString, searchableArticle.status.other.map(_.toString).toSeq)

      api.ArticleSummaryDTO(
        id = searchableArticle.id,
        title = title,
        visualElement = visualElement,
        introduction = introduction,
        url = ApplicationUrl.get + searchableArticle.id,
        license = searchableArticle.license.getOrElse(""),
        articleType = searchableArticle.articleType,
        supportedLanguages = supportedLanguages,
        tags = tag,
        notes = notes,
        users = users,
        grepCodes = searchableArticle.grepCodes,
        status = status,
        updated = searchableArticle.lastUpdated
      )
    }

    /** Attempts to extract language that hit from highlights in elasticsearch response.
      *
      * @param result
      *   Elasticsearch hit.
      * @return
      *   Language if found.
      */
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

      val highlightKeys: Option[Map[String, _]] = Option(result.highlight)
      val matchLanguage                         = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          keyToLanguage(result.sourceAsMap.keys)
      }
    }

    def asApiSearchResult(searchResult: SearchResult[api.ArticleSummaryDTO]): ArticleSearchResultDTO =
      api.ArticleSearchResultDTO(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.results
      )

    def tagSearchResultAsApiResult(searchResult: SearchResult[String]): api.TagsSearchResultDTO =
      api.TagsSearchResultDTO(
        searchResult.totalCount,
        searchResult.page.getOrElse(1),
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )

    def asSearchableTags(article: Draft): Seq[SearchableTag] = {
      article.tags.flatMap(articleTags =>
        articleTags.tags.map(tag =>
          SearchableTag(
            tag = tag,
            language = articleTags.language
          )
        )
      )
    }

    def asSearchableGrepCodes(article: Draft): Seq[SearchableGrepCode] =
      article.grepCodes.map(code => SearchableGrepCode(code))
  }
}
