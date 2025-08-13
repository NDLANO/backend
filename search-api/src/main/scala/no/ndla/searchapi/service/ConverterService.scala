/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service

import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.common.model
import no.ndla.common.model.api.search.{TitleDTO, TitleWithHtmlDTO}
import no.ndla.network.ApplicationUrl
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api.{
  ArticleResultDTO,
  ArticleResultsDTO,
  AudioResultDTO,
  AudioResultsDTO,
  ImageAltTextDTO,
  ImageResultDTO,
  ImageResultsDTO,
  LearningPathIntroductionDTO,
  LearningpathResultDTO,
  LearningpathResultsDTO,
  SearchResultsDTO
}
import no.ndla.searchapi.model.api.article.ArticleIntroductionDTO
import no.ndla.searchapi.model.domain.*

trait ConverterService {
  this: Props =>
  lazy val converterService: ConverterService

  class ConverterService {
    def searchResultToApiModel(searchResults: ApiSearchResults): SearchResultsDTO = {
      searchResults match {
        case a: ArticleApiSearchResults      => articleSearchResultsToApi(a)
        case l: LearningpathApiSearchResults => learningpathSearchResultsToApi(l)
        case i: ImageApiSearchResults        => imageSearchResultsToApi(i)
        case a: AudioApiSearchResults        => audioSearchResultsToApi(a)
      }
    }

    private def articleSearchResultsToApi(articles: ArticleApiSearchResults): ArticleResultsDTO = {
      ArticleResultsDTO(
        "articles",
        articles.language,
        articles.totalCount,
        articles.page,
        articles.pageSize,
        articles.results.map(articleSearchResultToApi)
      )
    }

    private def articleSearchResultToApi(article: ArticleApiSearchResult): ArticleResultDTO = {
      ArticleResultDTO(
        article.id,
        TitleWithHtmlDTO(article.title.title, article.title.htmlTitle, article.title.language),
        article.introduction.map(i => ArticleIntroductionDTO(i.introduction, i.htmlIntroduction, i.language)),
        article.articleType,
        article.supportedLanguages
      )
    }

    private def learningpathSearchResultsToApi(
        learningpaths: LearningpathApiSearchResults
    ): LearningpathResultsDTO = {
      LearningpathResultsDTO(
        "learningpaths",
        learningpaths.language,
        learningpaths.totalCount,
        learningpaths.page,
        learningpaths.pageSize,
        learningpaths.results.map(learningpathSearchResultToApi)
      )
    }

    private def learningpathSearchResultToApi(learningpath: LearningpathApiSearchResult): LearningpathResultDTO = {
      LearningpathResultDTO(
        learningpath.id,
        TitleDTO(learningpath.title.title, learningpath.title.language),
        LearningPathIntroductionDTO(learningpath.introduction.introduction, learningpath.introduction.language),
        learningpath.supportedLanguages
      )
    }

    private def imageSearchResultsToApi(images: ImageApiSearchResults): ImageResultsDTO = {
      ImageResultsDTO(
        "images",
        images.language,
        images.totalCount,
        images.page,
        images.pageSize,
        images.results.map(imageSearchResultToApi)
      )
    }

    private def imageSearchResultToApi(image: ImageApiSearchResult): ImageResultDTO = {
      val scheme = ApplicationUrl.get.schemeOption.getOrElse("https://")
      val host   = ApplicationUrl.get.hostOption.map(_.toString).getOrElse(props.Domain)

      val previewUrl = image.previewUrl.withHost(host).withScheme(scheme)
      val metaUrl    = image.metaUrl.withHost(host).withScheme(scheme)

      ImageResultDTO(
        image.id.toLong,
        TitleDTO(image.title.title, image.title.language),
        ImageAltTextDTO(image.altText.alttext, image.altText.language),
        previewUrl.toString,
        metaUrl.toString,
        image.supportedLanguages
      )
    }

    private def audioSearchResultsToApi(audios: AudioApiSearchResults): AudioResultsDTO = {
      AudioResultsDTO(
        "audios",
        audios.language,
        audios.totalCount,
        audios.page,
        audios.pageSize,
        audios.results.map(audioSearchResultToApi)
      )
    }

    private def audioSearchResultToApi(audio: AudioApiSearchResult): AudioResultDTO = {
      val scheme = ApplicationUrl.get.schemeOption.getOrElse("https://")
      val host   = ApplicationUrl.get.hostOption.map(_.toString).getOrElse(props.Domain)

      val url = audio.url.withHost(host).withScheme(scheme).toString
      AudioResultDTO(
        audio.id,
        model.api.search.TitleDTO(audio.title.title, audio.title.language),
        url,
        audio.supportedLanguages
      )
    }
  }
}
