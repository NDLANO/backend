/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.Elastic4sClientFactory
import no.ndla.search.model.SearchableLanguageFormats
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.model.search.SearchableArticle
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import org.json4s.Formats
import org.json4s.native.Serialization.read
import org.scalatest.Outcome

import scala.util.{Failure, Success}

class ArticleIndexServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {

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

  override val articleIndexService: ArticleIndexService = new ArticleIndexService {
    override val indexShards = 1
  }

  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService
  implicit val formats: Formats       = SearchableLanguageFormats.JSonFormatsWithMillis

  test("That articles are indexed correctly") {
    articleIndexService.cleanupIndexes()
    articleIndexService.createIndexWithGeneratedName

    articleIndexService.indexDocument(article5, TestData.taxonomyTestBundle, Some(TestData.emptyGrepBundle)).get
    articleIndexService.indexDocument(article6, TestData.taxonomyTestBundle, Some(TestData.emptyGrepBundle)).get
    articleIndexService.indexDocument(article7, TestData.taxonomyTestBundle, Some(TestData.emptyGrepBundle)).get

    blockUntil(() => { articleIndexService.countDocuments == 3 })

    val Success(response) = e4sClient.execute {
      search(articleIndexService.searchIndex)
    }

    val sources  = response.result.hits.hits.map(_.sourceAsString)
    val articles = sources.map(source => read[SearchableArticle](source))

    val Success(expectedArticle5) =
      searchConverterService.asSearchableArticle(article5, TestData.taxonomyTestBundle, Some(TestData.emptyGrepBundle))
    val Success(expectedArticle6) =
      searchConverterService.asSearchableArticle(article6, TestData.taxonomyTestBundle, Some(TestData.emptyGrepBundle))
    val Success(expectedArticle7) =
      searchConverterService.asSearchableArticle(article7, TestData.taxonomyTestBundle, Some(TestData.emptyGrepBundle))

    val Some(actualArticle5) = articles.find(p => p.id == article5.id.get)
    val Some(actualArticle6) = articles.find(p => p.id == article6.id.get)
    val Some(actualArticle7) = articles.find(p => p.id == article7.id.get)

    actualArticle5 should be(expectedArticle5)
    actualArticle6 should be(expectedArticle6)
    actualArticle7 should be(expectedArticle7)
  }
}
