/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service.search

import no.ndla.common.model.domain as common
import no.ndla.imageapi.model.domain.Sort
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.IntegrationSuite

import scala.util.Success
import no.ndla.imageapi.model.domain.ImageMetaInformation

class TagSearchServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  val indexName = "tags-testing"
  override val tagSearchService: TagSearchService = new TagSearchService {
    override val searchIndex: String = indexName
  }
  override val tagIndexService: TagIndexService = new TagIndexService {
    override val indexShards: Int    = 1
    override val searchIndex: String = indexName
  }
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  val image1: ImageMetaInformation = TestData.elg.copy(
    tags = Seq(
      common.Tag(
        Seq("test", "testing", "testemer"),
        "nb"
      )
    )
  )

  val image2: ImageMetaInformation = TestData.elg.copy(
    tags = Seq(
      common.Tag(
        Seq("test"),
        "en"
      )
    )
  )

  val image3: ImageMetaInformation = TestData.elg.copy(
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

  val image4: ImageMetaInformation = TestData.elg.copy(
    tags = Seq(
      common.Tag(
        Seq("kyllingfilet", "filetkylling"),
        "nb"
      )
    )
  )

  val imagesToIndex: Seq[ImageMetaInformation] = Seq(image1, image2, image3, image4)

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (elasticSearchContainer.isSuccess) {
      tagIndexService.createIndexAndAlias().get
      imagesToIndex.foreach(a => tagIndexService.indexDocument(a).get)

      val allTagsToIndex         = imagesToIndex.flatMap(_.tags)
      val groupedByLanguage      = allTagsToIndex.groupBy(_.language)
      val tagsDistinctByLanguage = groupedByLanguage.values.flatMap(x => x.flatMap(_.tags).toSet)

      blockUntil(() => tagSearchService.countDocuments() == tagsDistinctByLanguage.size)
    }
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
