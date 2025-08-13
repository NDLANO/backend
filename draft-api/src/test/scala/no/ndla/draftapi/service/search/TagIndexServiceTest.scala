/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service.search

import no.ndla.draftapi.*
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite

class TagIndexServiceTest extends ElasticsearchIntegrationSuite with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override lazy val tagIndexService: TagIndexService = new TagIndexService {
    override val indexShards = 1
  }
  override lazy val converterService       = new ConverterService
  override lazy val searchConverterService = new SearchConverterService

  test("That indexing does not fail if no tags are present") {
    tagIndexService.createIndexAndAlias()

    val article = TestData.sampleDomainArticle.copy(tags = Seq.empty)
    tagIndexService.indexDocument(article).failIfFailure

    tagIndexService.deleteIndexWithName(Some(props.DraftTagSearchIndex))
  }

}
