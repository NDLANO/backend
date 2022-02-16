/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.service.search

import no.ndla.audioapi.{AudioApiProperties, TestData, TestEnvironment}
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.Elastic4sClientFactory
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
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  val audio1: AudioMetaInformation = TestData.sampleAudio.copy(
    tags = Seq(
      domain.Tag(
        Seq("test", "testing", "testemer"),
        "nb"
      )
    )
  )

  val audio2: AudioMetaInformation = TestData.sampleAudio.copy(
    tags = Seq(
      domain.Tag(
        Seq("test"),
        "en"
      )
    )
  )

  val audio3: AudioMetaInformation = TestData.sampleAudio.copy(
    tags = Seq(
      domain.Tag(
        Seq("hei", "test", "testing"),
        "nb"
      ),
      domain.Tag(
        Seq("test"),
        "en"
      )
    )
  )

  val audio4: AudioMetaInformation = TestData.sampleAudio.copy(
    tags = Seq(
      domain.Tag(
        Seq("kyllingfilet", "filetkylling"),
        "nb"
      )
    )
  )

  val audiosToIndex = Seq(audio1, audio2, audio3, audio4)

  override def beforeAll(): Unit = if (elasticSearchContainer.isSuccess) {
    super.beforeAll()
    tagIndexService.createIndexWithName(AudioApiProperties.AudioTagSearchIndex)

    audiosToIndex.foreach(a => tagIndexService.indexDocument(a))

    val allTagsToIndex = audiosToIndex.flatMap(_.tags)
    val groupedByLanguage = allTagsToIndex.groupBy(_.language)
    val tagsDistinctByLanguage = groupedByLanguage.values.flatMap(x => x.flatMap(_.tags).toSet)

    blockUntil(() => tagSearchService.countDocuments == tagsDistinctByLanguage.size)
  }

  def blockUntil(predicate: () => Boolean): Unit = {
    var backoff = 0
    var done = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200 * backoff)
      backoff = backoff + 1
      try {
        done = predicate()
      } catch {
        case e: Throwable => println(("problem while testing predicate", e))
      }
    }

    require(done, s"Failed waiting for predicate")
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
