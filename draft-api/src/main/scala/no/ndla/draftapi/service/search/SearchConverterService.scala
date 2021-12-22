/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.http.search.SearchHit
import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.model.api.{AgreementSearchResult, ArticleSearchResult}
import no.ndla.draftapi.model.domain.Language._
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.model.search._
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.service.ConverterService
import no.ndla.mapping.ISO639
import no.ndla.network.ApplicationUrl
import org.joda.time.DateTime
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.read
import org.jsoup.Jsoup

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    def asSearchableArticle(ai: Article): SearchableArticle = {

      val defaultTitle = ai.title
        .sortBy(title => {
          val languagePriority = Language.languageAnalyzers.map(la => la.languageTag.toString).reverse
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
          ai.content.map(article => LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))),
        tags = SearchableLanguageList(ai.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = new DateTime(ai.updated),
        license = ai.copyright.flatMap(_.license),
        authors = ai.copyright
          .map(copy => copy.creators ++ copy.processors ++ copy.rightsholders)
          .map(a => a.map(_.name))
          .toSeq
          .flatten,
        articleType = ai.articleType.toString,
        notes = ai.notes.map(_.note),
        defaultTitle = defaultTitle.map(_.title),
        users = ai.updatedBy +: ai.notes.map(_.user),
        previousNotes = ai.previousVersionsNotes.map(_.note),
        grepCodes = ai.grepCodes
      )
    }

    def hitAsArticleSummary(hitString: String, language: String): api.ArticleSummary = {
      val searchableArticle = read[SearchableArticle](hitString)

      val titles = searchableArticle.title.languageValues.map(lv => domain.ArticleTitle(lv.value, lv.language))
      val introductions =
        searchableArticle.introduction.languageValues.map(lv => domain.ArticleIntroduction(lv.value, lv.language))
      val visualElements =
        searchableArticle.visualElement.languageValues.map(lv => domain.VisualElement(lv.value, lv.language))
      val tags = searchableArticle.tags.languageValues.map(lv => domain.ArticleTag(lv.value, lv.language))
      val notes = searchableArticle.notes
      val users = searchableArticle.users

      val supportedLanguages = getSupportedLanguages(Seq(titles, visualElements, introductions))

      val title = findByLanguageOrBestEffort(titles, language)
        .map(converterService.toApiArticleTitle)
        .getOrElse(api.ArticleTitle("", UnknownLanguage.toString))
      val visualElement = findByLanguageOrBestEffort(visualElements, language).map(converterService.toApiVisualElement)
      val introduction =
        findByLanguageOrBestEffort(introductions, language).map(converterService.toApiArticleIntroduction)
      val tag = findByLanguageOrBestEffort(tags, language).map(converterService.toApiArticleTag)

      api.ArticleSummary(
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
        grepCodes = searchableArticle.grepCodes
      )
    }

    def asSearchableAgreement(domainModel: Agreement): SearchableAgreement = {
      SearchableAgreement(
        id = domainModel.id.get,
        title = domainModel.title,
        content = domainModel.content,
        license = domainModel.copyright.license.get
      )
    }

    def hitAsAgreementSummary(hitString: String): api.AgreementSummary = {
      val hit = parse(hitString)
      val id = (hit \ "id").extract[Long]
      val title = (hit \ "title").extract[String]
      val license = (hit \ "license").extract[String]

      api.AgreementSummary(id, title, license)
    }

    /**
      * Attempts to extract language that hit from highlights in elasticsearch response.
      *
      * @param result Elasticsearch hit.
      * @return Language if found.
      */
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

    def asApiSearchResult(searchResult: SearchResult[api.ArticleSummary]): ArticleSearchResult =
      api.ArticleSearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.results
      )

    def tagSearchResultAsApiResult(searchResult: SearchResult[String]): api.TagsSearchResult =
      api.TagsSearchResult(
        searchResult.totalCount,
        searchResult.page.getOrElse(1),
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )

    def asApiAgreementSearchResult(searchResult: SearchResult[api.AgreementSummary]): AgreementSearchResult =
      api.AgreementSearchResult(searchResult.totalCount,
                                searchResult.page,
                                searchResult.pageSize,
                                searchResult.language,
                                searchResult.results)

    def asSearchableTags(article: domain.Article): Seq[SearchableTag] = {
      article.tags.flatMap(
        articleTags =>
          articleTags.tags.map(
            tag =>
              SearchableTag(
                tag = tag,
                language = articleTags.language
            )))
    }

    def asSearchableGrepCodes(article: domain.Article): Seq[SearchableGrepCode] =
      article.grepCodes.map(code => SearchableGrepCode(code))
  }
}
