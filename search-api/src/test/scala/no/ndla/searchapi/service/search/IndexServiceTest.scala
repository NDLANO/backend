/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.searchapi.TestEnvironment

import scala.util.Success

class IndexServiceTest extends ElasticsearchIntegrationSuite with TestEnvironment {
  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))

  val testIndexPrefix = "searchapi-index-service-test"

  val searchIndexService: ArticleIndexService = new ArticleIndexService {
    override val searchIndex = s"${testIndexPrefix}_article"
  }

  test("That cleanupIndexes does not delete others indexes") {
    val image1Name        = s"${testIndexPrefix}_image_1"
    val article1Name      = s"${testIndexPrefix}_article_1"
    val article2Name      = s"${testIndexPrefix}_article_2"
    val learningpath1Name = s"${testIndexPrefix}_learningpath_1"

    searchIndexService.createIndexWithName(image1Name)
    searchIndexService.createIndexWithName(article1Name)
    searchIndexService.createIndexWithName(article2Name)
    searchIndexService.createIndexWithName(learningpath1Name)
    searchIndexService.updateAliasTarget(None, article1Name)

    searchIndexService.cleanupIndexes(s"${testIndexPrefix}_article")

    val Success(response) = e4sClient.execute(getAliases())
    val result            = response.result.mappings
    val indexNames        = result.map(_._1.name)

    indexNames should contain(image1Name)
    indexNames should contain(article1Name)
    indexNames should contain(learningpath1Name)
    indexNames should not contain article2Name

    searchIndexService.cleanupIndexes(testIndexPrefix)
  }
}
