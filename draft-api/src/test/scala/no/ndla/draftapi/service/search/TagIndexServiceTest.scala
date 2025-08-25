/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service.search

import no.ndla.draftapi.*
import no.ndla.draftapi.service.ConverterService
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

class TagIndexServiceTest extends ElasticsearchIntegrationSuite with TestEnvironment {
  override implicit lazy val searchLanguage: SearchLanguage = new SearchLanguage
  override implicit lazy val e4sClient: NdlaE4sClient       =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override implicit lazy val tagIndexService: TagIndexService = new TagIndexService {
    override val indexShards = 1
  }
  override implicit lazy val converterService: ConverterService             = new ConverterService
  override implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService

  test("That indexing does not fail if no tags are present") {
    tagIndexService.createIndexAndAlias()

    val article = TestData.sampleDomainArticle.copy(tags = Seq.empty)
    tagIndexService.indexDocument(article).failIfFailure

    tagIndexService.deleteIndexWithName(Some(props.DraftTagSearchIndex))
  }

}
