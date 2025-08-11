/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service.search

import no.ndla.draftapi.*
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite

class GrepCodesIndexServiceTest extends ElasticsearchIntegrationSuite with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override lazy val grepCodesIndexService: GrepCodesIndexService = new GrepCodesIndexService {
    override val indexShards = 1
  }
  override lazy val converterService       = new ConverterService
  override lazy val searchConverterService = new SearchConverterService

  test("That indexing does not fail if no grepCodes are present") {
    tagIndexService.createIndexWithName(props.DraftGrepCodesSearchIndex)

    val article = TestData.sampleDomainArticle.copy(grepCodes = Seq.empty)
    grepCodesIndexService.indexDocument(article).isSuccess should be(true)

    grepCodesIndexService.deleteIndexWithName(Some(props.DraftGrepCodesSearchIndex))
  }

}
