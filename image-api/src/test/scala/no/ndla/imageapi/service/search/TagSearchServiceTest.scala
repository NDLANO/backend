/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import no.ndla.common.model.{domain => common}
import no.ndla.imageapi.model.domain.Sort
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.Elastic4sClientFactory
import org.scalatest.Outcome

import scala.util.Success

class TagSearchServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val tagSearchService = new TagSearchService
  override val tagIndexService: TagIndexService = new TagIndexService {
    override val indexShards: Int = 1
  }
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  val image1 = TestData.elg.copy(
    tags = Seq(
      common.Tag(
        Seq("test", "testing", "testemer"),
        "nb"
      )
    )
  )

  val image2 = TestData.elg.copy(
    tags = Seq(
      common.Tag(
        Seq("test"),
        "en"
      )
    )
  )

  val image3 = TestData.elg.copy(
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

  val image4 = TestData.elg.copy(
    tags = Seq(
      common.Tag(
        Seq("kyllingfilet", "filetkylling"),
        "nb"
      )
    )
  )

  val imagesToIndex = Seq(image1, image2, image3, image4)

  override def beforeAll(): Unit = if (elasticSearchContainer.isSuccess) {
    val indexName = tagIndexService.createIndexWithGeneratedName
    tagIndexService.updateAliasTarget(None, indexName.get)

    imagesToIndex.foreach(a => {
      val x = tagIndexService.indexDocument(a)
      x
    })

    val allTagsToIndex         = imagesToIndex.flatMap(_.tags)
    val groupedByLanguage      = allTagsToIndex.groupBy(_.language)
    val tagsDistinctByLanguage = groupedByLanguage.values.flatMap(x => x.flatMap(_.tags).toSet)

    blockUntil(() => tagSearchService.countDocuments() == tagsDistinctByLanguage.size)
  }

  test("That searching for tags returns sensible results") {
    val Success(result) = tagSearchService.matchingQuery("test", "nb", 1, 100, Sort.ByRelevanceDesc)

    result.totalCount should be(3)
    result.results should be(Seq("test", "testemer", "testing"))
  }

  test("That only prefixes are matched") {
    val Success(result) = tagSearchService.matchingQuery("kylling", "nb", 1, 100, Sort.ByRelevanceDesc)

    result.totalCount should be(1)
    result.results should be(Seq("kyllingfilet"))
  }

}
