/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service.search

import no.ndla.common.model.domain.*
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.*
import no.ndla.draftapi.service.ConverterService
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

import scala.util.Success

class TagSearchServiceTest extends ElasticsearchIntegrationSuite with TestEnvironment {
  override implicit lazy val searchLanguage: SearchLanguage = new SearchLanguage
  override implicit lazy val e4sClient: NdlaE4sClient       =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override implicit lazy val tagSearchService: TagSearchService = new TagSearchService
  override implicit lazy val tagIndexService: TagIndexService   = new TagIndexService {
    override val indexShards = 1
  }
  override implicit lazy val converterService: ConverterService             = new ConverterService
  override implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService

  val article1: Draft = TestData.sampleDomainArticle.copy(
    tags = Seq(
      Tag(
        Seq("test", "testing", "testemer"),
        "nb"
      )
    )
  )

  val article2: Draft = TestData.sampleDomainArticle.copy(
    tags = Seq(
      Tag(
        Seq("test"),
        "en"
      )
    )
  )

  val article3: Draft = TestData.sampleDomainArticle.copy(
    tags = Seq(
      Tag(
        Seq("hei", "test", "testing"),
        "nb"
      ),
      Tag(
        Seq("test"),
        "en"
      )
    )
  )

  val article4: Draft = TestData.sampleDomainArticle.copy(
    tags = Seq(
      Tag(
        Seq("kyllingfilet", "filetkylling"),
        "nb"
      )
    )
  )

  val articlesToIndex: Seq[Draft] = Seq(article1, article2, article3, article4)

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (elasticSearchContainer.isSuccess) {
      tagIndexService.createIndexAndAlias().get

      articlesToIndex.foreach(a => tagIndexService.indexDocument(a).get)

      val allTagsToIndex         = articlesToIndex.flatMap(_.tags)
      val groupedByLanguage      = allTagsToIndex.groupBy(_.language)
      val tagsDistinctByLanguage = groupedByLanguage.values.flatMap(x => x.flatMap(_.tags).toSet)

      blockUntil(() => tagSearchService.countDocuments == tagsDistinctByLanguage.size)
    }
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
