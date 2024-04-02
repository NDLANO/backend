/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import no.ndla.draftapi._
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome

class GrepCodesIndexServiceTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val grepCodesIndexService: GrepCodesIndexService = new GrepCodesIndexService {
    override val indexShards = 1
  }
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  test("That indexing does not fail if no grepCodes are present") {
    tagIndexService.createIndexWithName(props.DraftGrepCodesSearchIndex)

    val article = TestData.sampleDomainArticle.copy(grepCodes = Seq.empty)
    grepCodesIndexService.indexDocument(article).isSuccess should be(true)

    grepCodesIndexService.deleteIndexWithName(Some(props.DraftGrepCodesSearchIndex))
  }

}
