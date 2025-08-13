/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service

import no.ndla.network.ApplicationUrl
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.model.api

class ConverterServiceTest extends UnitSuite with TestEnvironment {
  ApplicationUrl.applicationUrl.set("https://unit-test")
  override lazy val converterService = new ConverterService

  test("searchResultToApiModel should return the api model of the corresponding input domain model") {
    converterService.searchResultToApiModel(TestData.sampleArticleSearch).isInstanceOf[api.ArticleResultsDTO]
    converterService.searchResultToApiModel(TestData.sampleLearningpath).isInstanceOf[api.LearningpathResultsDTO]
    converterService.searchResultToApiModel(TestData.sampleImageSearch).isInstanceOf[api.ImageResultsDTO]
    converterService.searchResultToApiModel(TestData.sampleAudio).isInstanceOf[api.AudioResultsDTO]
  }
}
