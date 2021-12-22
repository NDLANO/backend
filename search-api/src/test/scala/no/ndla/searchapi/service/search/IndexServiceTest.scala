/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.searchapi.integration.Elastic4sClientFactory
import no.ndla.searchapi.TestEnvironment
import org.scalatest.Outcome

import scala.util.{Failure, Success}

class IndexServiceTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {
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

  val testIndexPrefix = "searchapi-index-service-test"

  val searchIndexService = new ArticleIndexService {
    override val searchIndex = s"${testIndexPrefix}_article"
  }

  test("That cleanupIndexes does not delete others indexes") {
    val image1Name = s"${testIndexPrefix}_image_1"
    val article1Name = s"${testIndexPrefix}_article_1"
    val article2Name = s"${testIndexPrefix}_article_2"
    val learningpath1Name = s"${testIndexPrefix}_learningpath_1"

    searchIndexService.createIndexWithName(image1Name)
    searchIndexService.createIndexWithName(article1Name)
    searchIndexService.createIndexWithName(article2Name)
    searchIndexService.createIndexWithName(learningpath1Name)
    searchIndexService.updateAliasTarget(None, article1Name)

    searchIndexService.cleanupIndexes(s"${testIndexPrefix}_article")

    val Success(response) = e4sClient.execute(getAliases())
    val result = response.result.mappings
    val indexNames = result.map(_._1.name)

    indexNames should contain(image1Name)
    indexNames should contain(article1Name)
    indexNames should contain(learningpath1Name)
    indexNames should not contain article2Name

    searchIndexService.cleanupIndexes(testIndexPrefix)
  }
}
