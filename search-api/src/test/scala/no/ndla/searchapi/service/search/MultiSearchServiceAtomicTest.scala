/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.searchapi.TestData.blockUntil
import no.ndla.searchapi.integration.Elastic4sClientFactory
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.{SearchApiProperties, TestData, TestEnvironment}
import org.scalatest.Outcome

import scala.util.{Failure, Success}

class MultiSearchServiceAtomicTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {
  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    elasticSearchContainer match {
      case Failure(ex) =>
        println(s"Elasticsearch container not running, cancelling '${this.getClass.getName}'")
        println(s"Got exception: ${ex.getMessage}")
        ex.printStackTrace()
      case _ =>
    }

    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val articleIndexService = new ArticleIndexService
  override val draftIndexService = new DraftIndexService
  override val learningPathIndexService = new LearningPathIndexService
  override val multiSearchService = new MultiSearchService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  override def beforeEach(): Unit = {
    if (elasticSearchContainer.isSuccess) {
      articleIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.Articles))
      draftIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.Drafts))
      learningPathIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.LearningPaths))
    }
  }

  override def afterEach(): Unit = {
    if (elasticSearchContainer.isSuccess) {
      articleIndexService.deleteIndexWithName(Some(SearchApiProperties.SearchIndexes(SearchType.Articles)))
      draftIndexService.deleteIndexWithName(Some(SearchApiProperties.SearchIndexes(SearchType.Drafts)))
      learningPathIndexService.deleteIndexWithName(Some(SearchApiProperties.SearchIndexes(SearchType.LearningPaths)))
    }
  }

  test("That search on embed id supports embed with multiple resources") {
    val article1 = TestData.article1.copy(
      id = Some(1),
      content = Seq(
        ArticleContent(
          """<section><div data-type="related-content"><embed data-article-id="3" data-resource="related-content"></div></section>""",
          "nb"
        )
      )
    )
    val article2 = TestData.article1.copy(
      id = Some(2),
      content = Seq(
        ArticleContent(
          """<section><embed data-content-id="3" data-link-text="Test?" data-resource="content-link"></section>""",
          "nb"
        )
      )
    )
    val article3 = TestData.article1.copy(id = Some(3))
    articleIndexService.indexDocument(article1, TestData.taxonomyTestBundle, Some(TestData.grepBundle)).get
    articleIndexService.indexDocument(article2, TestData.taxonomyTestBundle, Some(TestData.grepBundle)).get
    articleIndexService.indexDocument(article3, TestData.taxonomyTestBundle, Some(TestData.grepBundle)).get

    blockUntil(() => {
      articleIndexService.countDocuments == 3
    })

    val Success(search1) =
      multiSearchService.matchingQuery(
        TestData.searchSettings.copy(embedId = Some("3"), embedResource = List("content-link"))
      )

    search1.totalCount should be(1)
    search1.results.map(_.id) should be(List(2))

    val Success(search2) =
      multiSearchService.matchingQuery(
        TestData.searchSettings.copy(embedId = Some("3"), embedResource = List("content-link", "related-content"))
      )

    search2.totalCount should be(2)
    search2.results.map(_.id) should be(List(1, 2))

  }

}
