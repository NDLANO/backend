/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.service.search

import no.ndla.audioapi.{TestData, TestEnvironment}
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.common.model.{domain => common}
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome

import scala.util.Success

class TagSearchServiceTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val tagSearchService = new TagSearchService
  override val tagIndexService: TagIndexService = new TagIndexService {
    override val indexShards = 1
  }
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  val audio1: AudioMetaInformation = TestData.sampleAudio.copy(
    tags = Seq(
      common.Tag(
        Seq("test", "testing", "testemer"),
        "nb"
      )
    )
  )

  val audio2: AudioMetaInformation = TestData.sampleAudio.copy(
    tags = Seq(
      common.Tag(
        Seq("test"),
        "en"
      )
    )
  )

  val audio3: AudioMetaInformation = TestData.sampleAudio.copy(
    tags = Seq(
      common.Tag(
        Seq("hei", "test", "testing"),
        "nb"
      ),
      common.Tag(
        Seq("test"),
        "en"
      )
    )
  )

  val audio4: AudioMetaInformation = TestData.sampleAudio.copy(
    tags = Seq(
      common.Tag(
        Seq("kyllingfilet", "filetkylling"),
        "nb"
      )
    )
  )

  val audiosToIndex = Seq(audio1, audio2, audio3, audio4)

  override def beforeAll(): Unit = if (elasticSearchContainer.isSuccess) {
    super.beforeAll()
    tagIndexService.createIndexWithName(props.AudioTagSearchIndex)

    audiosToIndex.foreach(a => tagIndexService.indexDocument(a))

    val allTagsToIndex         = audiosToIndex.flatMap(_.tags)
    val groupedByLanguage      = allTagsToIndex.groupBy(_.language)
    val tagsDistinctByLanguage = groupedByLanguage.values.flatMap(x => x.flatMap(_.tags).toSet)

    blockUntil(() => tagSearchService.countDocuments == tagsDistinctByLanguage.size)
  }

  test("That searching for tags returns sensible results") {
    val Success(result) = tagSearchService.matchingQuery("test", "nb", 1, 100)

    result.totalCount should be(3)
    result.results should be(Seq("test", "testemer", "testing"))
  }

  test("That only prefixes are matched") {
    val Success(result) = tagSearchService.matchingQuery("kylling", "nb", 1, 100)

    result.totalCount should be(1)
    result.results should be(Seq("kyllingfilet"))
  }

}
