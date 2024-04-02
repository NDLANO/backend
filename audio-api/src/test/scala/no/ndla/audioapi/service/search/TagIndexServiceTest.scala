/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.service.search

import no.ndla.audioapi.{TestData, TestEnvironment}
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome

class TagIndexServiceTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

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
