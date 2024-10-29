/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service

import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.network.ApplicationUrl
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.api.LearningPathIntroduction
import no.ndla.searchapi.model.api.article.ArticleIntroduction
import no.ndla.searchapi.model.domain.*

trait ConverterService {
  this: Props =>
  val converterService: ConverterService

  class ConverterService {
    import props.Domain

    def searchResultToApiModel(searchResults: ApiSearchResults): api.SearchResults = {
      searchResults match {
        case a: ArticleApiSearchResults      => articleSearchResultsToApi(a)
        case l: LearningpathApiSearchResults => learningpathSearchResultsToApi(l)
        case i: ImageApiSearchResults        => imageSearchResultsToApi(i)
        case a: AudioApiSearchResults        => audioSearchResultsToApi(a)
      }
    }

    private def articleSearchResultsToApi(articles: ArticleApiSearchResults): api.ArticleResults = {
      api.ArticleResults(
        "articles",
        articles.language,
        articles.totalCount,
        articles.page,
        articles.pageSize,
        articles.results.map(articleSearchResultToApi)
      )
    }

    private def articleSearchResultToApi(article: ArticleApiSearchResult): api.ArticleResult = {
      api.ArticleResult(
        article.id,
        api.Title(article.title.title, article.title.htmlTitle, article.title.language),
        article.introduction.map(i => ArticleIntroduction(i.introduction, i.htmlIntroduction, i.language)),
        article.articleType,
        article.supportedLanguages
      )
    }

    private def learningpathSearchResultsToApi(learningpaths: LearningpathApiSearchResults): api.LearningpathResults = {
      api.LearningpathResults(
        "learningpaths",
        learningpaths.language,
        learningpaths.totalCount,
        learningpaths.page,
        learningpaths.pageSize,
        learningpaths.results.map(learningpathSearchResultToApi)
      )
    }

    private def learningpathSearchResultToApi(learningpath: LearningpathApiSearchResult): api.LearningpathResult = {
      api.LearningpathResult(
        learningpath.id,
        api.Title(learningpath.title.title, learningpath.title.title, learningpath.title.language),
        LearningPathIntroduction(learningpath.introduction.introduction, learningpath.introduction.language),
        learningpath.supportedLanguages
      )
    }

    private def imageSearchResultsToApi(images: ImageApiSearchResults): api.ImageResults = {
      api.ImageResults(
        "images",
        images.language,
        images.totalCount,
        images.page,
        images.pageSize,
        images.results.map(imageSearchResultToApi)
      )
    }

    private def imageSearchResultToApi(image: ImageApiSearchResult): api.ImageResult = {
      val scheme = ApplicationUrl.get.schemeOption.getOrElse("https://")
      val host   = ApplicationUrl.get.hostOption.map(_.toString).getOrElse(Domain)

      val previewUrl = image.previewUrl.withHost(host).withScheme(scheme)
      val metaUrl    = image.metaUrl.withHost(host).withScheme(scheme)

      api.ImageResult(
        image.id.toLong,
        api.Title(image.title.title, image.title.title, image.title.language),
        api.ImageAltText(image.altText.alttext, image.altText.language),
        previewUrl.toString,
        metaUrl.toString,
        image.supportedLanguages
      )
    }

    private def audioSearchResultsToApi(audios: AudioApiSearchResults): api.AudioResults = {
      api.AudioResults(
        "audios",
        audios.language,
        audios.totalCount,
        audios.page,
        audios.pageSize,
        audios.results.map(audioSearchResultToApi)
      )
    }

    private def audioSearchResultToApi(audio: AudioApiSearchResult): api.AudioResult = {
      val scheme = ApplicationUrl.get.schemeOption.getOrElse("https://")
      val host   = ApplicationUrl.get.hostOption.map(_.toString).getOrElse(Domain)

      val url = audio.url.withHost(host).withScheme(scheme).toString
      api.AudioResult(
        audio.id,
        api.Title(audio.title.title, audio.title.title, audio.title.language),
        url,
        audio.supportedLanguages
      )
    }
  }
}
