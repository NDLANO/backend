/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import no.ndla.draftapi._
import no.ndla.draftapi.integration.Elastic4sClientFactory
import no.ndla.draftapi.model.domain
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome

import scala.util.Success

class TagIndexServiceTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val tagIndexService = new TagIndexService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  def blockUntil(predicate: () => Boolean): Unit = {
    var backoff = 0
    var done = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200 * backoff)
      backoff = backoff + 1
      try {
        done = predicate()
      } catch {
        case e: Throwable => println("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting for predicate")
  }

  test("That indexing does not fail if no tags are present") {
    tagIndexService.createIndexWithName(DraftApiProperties.DraftTagSearchIndex)

    val article = TestData.sampleDomainArticle.copy(tags = Seq.empty)
    tagIndexService.indexDocument(article).isSuccess should be(true)

    tagIndexService.deleteIndexWithName(Some(DraftApiProperties.DraftTagSearchIndex))
  }

}
