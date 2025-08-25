/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import no.ndla.audioapi.service.ConverterService
import no.ndla.audioapi.{TestData, TestEnvironment}
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

class TagIndexServiceTest extends ElasticsearchIntegrationSuite with TestEnvironment {

  override implicit lazy val e4sClient: NdlaE4sClient =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override implicit lazy val searchLanguage: SearchLanguage   = new SearchLanguage
  override implicit lazy val tagIndexService: TagIndexService = new TagIndexService {
    override val indexShards = 1
  }
  override implicit lazy val converterService: ConverterService             = new ConverterService
  override implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService

  test("That indexing does not fail if no tags are present") {
    tagIndexService.createIndexAndAlias()

    val audio = TestData.sampleAudio.copy(tags = Seq.empty)
    tagIndexService.indexDocument(audio).isSuccess should be(true)

    tagIndexService.deleteIndexWithName(Some(props.AudioTagSearchIndex))
  }

}
