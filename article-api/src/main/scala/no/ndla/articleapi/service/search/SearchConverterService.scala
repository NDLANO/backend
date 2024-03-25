/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.model.api.{ArticleSummaryV2, SearchResultV2}
import no.ndla.articleapi.model.search._
import no.ndla.articleapi.service.ConverterService
import no.ndla.common.model.domain.article.Article
import no.ndla.search.SearchLanguage.languageAnalyzers
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import org.jsoup.Jsoup

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends StrictLogging {

    def asSearchableArticle(ai: Article): SearchableArticle = {
      val defaultTitle = ai.title
        .sortBy(title => {
          val languagePriority = languageAnalyzers.map(la => la.languageTag.toString).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      SearchableArticle(
        id = ai.id.get,
        title = SearchableLanguageValues(ai.title.map(title => LanguageValue(title.language, title.title))),
        visualElement = SearchableLanguageValues(
          ai.visualElement.map(visual => LanguageValue(visual.language, visual.resource))
        ),
        introduction = SearchableLanguageValues(
          ai.introduction.map(intro => LanguageValue(intro.language, intro.introduction))
        ),
        metaDescription = SearchableLanguageValues(
          ai.metaDescription.map(meta => LanguageValue(meta.language, meta.content))
        ),
        metaImage = ai.metaImage,
        content = SearchableLanguageValues(
          ai.content.map(article => LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))
        ),
        tags = SearchableLanguageList(ai.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = ai.updated,
        license = ai.copyright.license,
        authors = ai.copyright.creators.map(_.name) ++ ai.copyright.processors
          .map(_.name) ++ ai.copyright.rightsholders.map(_.name),
        articleType = ai.articleType.entryName,
        defaultTitle = defaultTitle.map(t => t.title),
        grepCodes = Some(ai.grepCodes),
        availability = ai.availability.toString
      )
    }

    def asApiSearchResultV2(searchResult: SearchResult[ArticleSummaryV2]): SearchResultV2 =
      SearchResultV2(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )

  }
}
