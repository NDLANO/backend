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
import no.ndla.searchapi.model.api.LearningPathIntroductionDTO
import no.ndla.searchapi.model.api.article.ArticleIntroductionDTO
import no.ndla.searchapi.model.domain.*

trait ConverterService {
  this: Props =>
  val converterService: ConverterService

  class ConverterService {
    import props.Domain

    def searchResultToApiModel(searchResults: ApiSearchResults): api.SearchResultsDTO = {
      searchResults match {
        case a: ArticleApiSearchResults      => articleSearchResultsToApi(a)
        case l: LearningpathApiSearchResults => learningpathSearchResultsToApi(l)
        case i: ImageApiSearchResults        => imageSearchResultsToApi(i)
        case a: AudioApiSearchResults        => audioSearchResultsToApi(a)
      }
    }

    private def articleSearchResultsToApi(articles: ArticleApiSearchResults): api.ArticleResultsDTO = {
      api.ArticleResultsDTO(
        "articles",
        articles.language,
        articles.totalCount,
        articles.page,
        articles.pageSize,
        articles.results.map(articleSearchResultToApi)
      )
    }

    private def articleSearchResultToApi(article: ArticleApiSearchResult): api.ArticleResultDTO = {
      api.ArticleResultDTO(
        article.id,
        api.TitleWithHtmlDTO(article.title.title, article.title.htmlTitle, article.title.language),
        article.introduction.map(i => ArticleIntroductionDTO(i.introduction, i.htmlIntroduction, i.language)),
        article.articleType,
        article.supportedLanguages
      )
    }

    private def learningpathSearchResultsToApi(
        learningpaths: LearningpathApiSearchResults
    ): api.LearningpathResultsDTO = {
      api.LearningpathResultsDTO(
        "learningpaths",
        learningpaths.language,
        learningpaths.totalCount,
        learningpaths.page,
        learningpaths.pageSize,
        learningpaths.results.map(learningpathSearchResultToApi)
      )
    }

    private def learningpathSearchResultToApi(learningpath: LearningpathApiSearchResult): api.LearningpathResultDTO = {
      api.LearningpathResultDTO(
        learningpath.id,
        api.TitleDTO(learningpath.title.title, learningpath.title.language),
        LearningPathIntroductionDTO(learningpath.introduction.introduction, learningpath.introduction.language),
        learningpath.supportedLanguages
      )
    }

    private def imageSearchResultsToApi(images: ImageApiSearchResults): api.ImageResultsDTO = {
      api.ImageResultsDTO(
        "images",
        images.language,
        images.totalCount,
        images.page,
        images.pageSize,
        images.results.map(imageSearchResultToApi)
      )
    }

    private def imageSearchResultToApi(image: ImageApiSearchResult): api.ImageResultDTO = {
      val scheme = ApplicationUrl.get.schemeOption.getOrElse("https://")
      val host   = ApplicationUrl.get.hostOption.map(_.toString).getOrElse(Domain)

      val previewUrl = image.previewUrl.withHost(host).withScheme(scheme)
      val metaUrl    = image.metaUrl.withHost(host).withScheme(scheme)

      api.ImageResultDTO(
        image.id.toLong,
        api.TitleDTO(image.title.title, image.title.language),
        api.ImageAltTextDTO(image.altText.alttext, image.altText.language),
        previewUrl.toString,
        metaUrl.toString,
        image.supportedLanguages
      )
    }

    private def audioSearchResultsToApi(audios: AudioApiSearchResults): api.AudioResultsDTO = {
      api.AudioResultsDTO(
        "audios",
        audios.language,
        audios.totalCount,
        audios.page,
        audios.pageSize,
        audios.results.map(audioSearchResultToApi)
      )
    }

    private def audioSearchResultToApi(audio: AudioApiSearchResult): api.AudioResultDTO = {
      val scheme = ApplicationUrl.get.schemeOption.getOrElse("https://")
      val host   = ApplicationUrl.get.hostOption.map(_.toString).getOrElse(Domain)

      val url = audio.url.withHost(host).withScheme(scheme).toString
      api.AudioResultDTO(
        audio.id,
        api.TitleDTO(audio.title.title, audio.title.language),
        url,
        audio.supportedLanguages
      )
    }
  }
}
