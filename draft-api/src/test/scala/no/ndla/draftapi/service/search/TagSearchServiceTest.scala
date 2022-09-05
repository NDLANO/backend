/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import no.ndla.common.model.domain._
import no.ndla.draftapi._
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
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  val article1 = TestData.sampleDomainArticle.copy(
    tags = Seq(
      Tag(
        Seq("test", "testing", "testemer"),
        "nb"
      )
    )
  )

  val article2 = TestData.sampleDomainArticle.copy(
    tags = Seq(
      Tag(
        Seq("test"),
        "en"
      )
    )
  )

  val article3 = TestData.sampleDomainArticle.copy(
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

  val article4 = TestData.sampleDomainArticle.copy(
    tags = Seq(
      Tag(
        Seq("kyllingfilet", "filetkylling"),
        "nb"
      )
    )
  )

  val articlesToIndex = Seq(article1, article2, article3, article4)

  override def beforeAll(): Unit = if (elasticSearchContainer.isSuccess) {
    tagIndexService.createIndexWithName(props.DraftTagSearchIndex)

    articlesToIndex.foreach(a => tagIndexService.indexDocument(a))

    val allTagsToIndex         = articlesToIndex.flatMap(_.tags)
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
