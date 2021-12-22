/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.api.{ArticleSummaryV2, SearchResultV2}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.search._
import no.ndla.articleapi.service.ConverterService
import no.ndla.network.ApplicationUrl
import org.joda.time.DateTime
import org.jsoup.Jsoup

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableArticle(ai: Article): SearchableArticle = {
      val articleWithAgreement = converterService.withAgreementCopyright(ai)

      val defaultTitle = articleWithAgreement.title
        .sortBy(title => {
          val languagePriority = Language.languageAnalyzers.map(la => la.languageTag.toString).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      SearchableArticle(
        id = articleWithAgreement.id.get,
        title =
          SearchableLanguageValues(articleWithAgreement.title.map(title => LanguageValue(title.language, title.title))),
        visualElement = SearchableLanguageValues(
          articleWithAgreement.visualElement.map(visual => LanguageValue(visual.language, visual.resource))),
        introduction = SearchableLanguageValues(
          articleWithAgreement.introduction.map(intro => LanguageValue(intro.language, intro.introduction))),
        metaDescription = SearchableLanguageValues(
          articleWithAgreement.metaDescription.map(meta => LanguageValue(meta.language, meta.content))),
        metaImage = articleWithAgreement.metaImage,
        content = SearchableLanguageValues(articleWithAgreement.content.map(article =>
          LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))),
        tags = SearchableLanguageList(articleWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = new DateTime(articleWithAgreement.updated),
        license = articleWithAgreement.copyright.license,
        authors = articleWithAgreement.copyright.creators.map(_.name) ++ articleWithAgreement.copyright.processors
          .map(_.name) ++ articleWithAgreement.copyright.rightsholders.map(_.name),
        articleType = articleWithAgreement.articleType,
        defaultTitle = defaultTitle.map(t => t.title),
        grepCodes = articleWithAgreement.grepCodes,
        availability = articleWithAgreement.availability.toString
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
