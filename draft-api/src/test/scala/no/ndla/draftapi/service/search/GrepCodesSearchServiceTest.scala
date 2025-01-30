/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import no.ndla.draftapi._
import no.ndla.scalatestsuite.IntegrationSuite

import scala.util.Success
import no.ndla.common.model.domain.draft.Draft

class GrepCodesSearchServiceTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override val grepCodesSearchService = new GrepCodesSearchService
  override val grepCodesIndexService: GrepCodesIndexService = new GrepCodesIndexService {
    override val indexShards: Int = 1
  }
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  val article1: Draft = TestData.sampleDomainArticle.copy(
    grepCodes = Seq("KE101", "KE115", "TT555")
  )

  val article2: Draft = TestData.sampleDomainArticle.copy(
    grepCodes = Seq("KE105")
  )

  val article3: Draft = TestData.sampleDomainArticle.copy(
    grepCodes = Seq("KM105")
  )

  val article4: Draft = TestData.sampleDomainArticle.copy(
    grepCodes = Seq()
  )

  val articlesToIndex: Seq[Draft] = Seq(article1, article2, article3, article4)

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (elasticSearchContainer.isSuccess) {
      tagIndexService.createIndexWithName(props.DraftGrepCodesSearchIndex)

      articlesToIndex.foreach(a => grepCodesIndexService.indexDocument(a))

      val allGrepCodesToIndex = articlesToIndex.flatMap(_.grepCodes)

      blockUntil(() => grepCodesSearchService.countDocuments == allGrepCodesToIndex.size)
    }
  }

  test("That searching for grepcodes returns sensible results") {
    val Success(result) = grepCodesSearchService.matchingQuery("KE", 1, 100)

    result.totalCount should be(3)
    result.results should be(Seq("KE101", "KE115", "KE105"))

    val Success(result2) = grepCodesSearchService.matchingQuery("KE115", 1, 100)

    result2.totalCount should be(1)
    result2.results should be(Seq("KE115"))
  }

  test("That searching for grepcodes returns sensible results even if lowercase") {
    val Success(result) = grepCodesSearchService.matchingQuery("ke", 1, 100)

    result.totalCount should be(3)
    result.results should be(Seq("KE101", "KE115", "KE105"))

    val Success(result2) = grepCodesSearchService.matchingQuery("ke115", 1, 100)

    result2.totalCount should be(1)
    result2.results should be(Seq("KE115"))
  }

  test("That only prefixes are matched with grepcodes") {
    val Success(result) = grepCodesSearchService.matchingQuery("TT", 1, 100)

    result.totalCount should be(1)
    result.results should be(Seq("TT555"))
  }

}
