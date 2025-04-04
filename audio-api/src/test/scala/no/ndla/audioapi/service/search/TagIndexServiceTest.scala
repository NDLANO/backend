/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import no.ndla.audioapi.{TestData, TestEnvironment}
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite

class TagIndexServiceTest extends ElasticsearchIntegrationSuite with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override val tagIndexService: TagIndexService = new TagIndexService {
    override val indexShards = 1
  }
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  test("That indexing does not fail if no tags are present") {
    tagIndexService.createIndexAndAlias()

    val audio = TestData.sampleAudio.copy(tags = Seq.empty)
    tagIndexService.indexDocument(audio).isSuccess should be(true)

    tagIndexService.deleteIndexWithName(Some(props.AudioTagSearchIndex))
  }

}
