/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import no.ndla.audioapi.controller.ControllerErrorHandling
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.service.ConverterService
import no.ndla.audioapi.{TestData, TestEnvironment}
import no.ndla.common.model.domain as common
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

import scala.util.Success

class TagSearchServiceTest extends ElasticsearchIntegrationSuite with TestEnvironment {

  override implicit lazy val e4sClient: NdlaE4sClient =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override implicit lazy val searchLanguage: SearchLanguage         = new SearchLanguage
  override implicit lazy val errorHandling: ControllerErrorHandling = new ControllerErrorHandling
  override implicit lazy val tagSearchService: TagSearchService     = new TagSearchService
  override implicit lazy val tagIndexService: TagIndexService       = new TagIndexService {
    override val indexShards = 1
  }
  override implicit lazy val converterService: ConverterService             = new ConverterService
  override implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService

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

  val audiosToIndex: Seq[AudioMetaInformation] = Seq(audio1, audio2, audio3, audio4)

  override def beforeAll(): Unit = if (elasticSearchContainer.isSuccess) {
    super.beforeAll()
    tagIndexService.createIndexAndAlias().get

    audiosToIndex.foreach(a => tagIndexService.indexDocument(a).get)

    val allTagsToIndex         = audiosToIndex.flatMap(_.tags)
    val groupedByLanguage      = allTagsToIndex.groupBy(_.language)
    val tagsDistinctByLanguage = groupedByLanguage.values.flatMap(x => x.flatMap(_.tags).toSet)

    blockUntil(() => tagSearchService.countDocuments == tagsDistinctByLanguage.size)
  }

  test("That searching for tags returns sensible results") {
    val Success(result) = tagSearchService.matchingQuery("test", "nb", 1, 100): @unchecked

    result.totalCount should be(3)
    result.results should be(Seq("test", "testemer", "testing"))
  }

  test("That only prefixes are matched") {
    val Success(result) = tagSearchService.matchingQuery("kylling", "nb", 1, 100): @unchecked

    result.totalCount should be(1)
    result.results should be(Seq("kyllingfilet"))
  }

}
