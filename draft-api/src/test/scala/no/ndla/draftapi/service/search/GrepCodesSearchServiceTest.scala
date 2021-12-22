/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import no.ndla.draftapi._
import no.ndla.draftapi.integration.Elastic4sClientFactory
import no.ndla.draftapi.model.domain
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome

import scala.util.Success

class GrepCodesSearchServiceTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val grepCodesSearchService = new GrepCodesSearchService
  override val grepCodesIndexService = new GrepCodesIndexService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  val article1 = TestData.sampleDomainArticle.copy(
    grepCodes = Seq("KE101", "KE115", "TT555")
  )

  val article2 = TestData.sampleDomainArticle.copy(
    grepCodes = Seq("KE105")
  )

  val article3 = TestData.sampleDomainArticle.copy(
    grepCodes = Seq("KM105")
  )

  val article4 = TestData.sampleDomainArticle.copy(
    grepCodes = Seq()
  )

  val articlesToIndex = Seq(article1, article2, article3, article4)

  override def beforeAll(): Unit = if (elasticSearchContainer.isSuccess) {
    tagIndexService.createIndexWithName(DraftApiProperties.DraftGrepCodesSearchIndex)

    articlesToIndex.foreach(a => grepCodesIndexService.indexDocument(a))

    val allGrepCodesToIndex = articlesToIndex.flatMap(_.grepCodes)

    blockUntil(() => grepCodesSearchService.countDocuments == allGrepCodesToIndex.size)
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
        case e: Throwable => println("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting for predicate")
  }

  test("That searching for grepcodes returns sensible results") {
    val Success(result) = grepCodesSearchService.matchingQuery("KE", 1, 100)

    result.totalCount should be(3)
    result.results should be(Seq("KE101", "KE105", "KE115"))

    val Success(result2) = grepCodesSearchService.matchingQuery("KE115", 1, 100)

    result2.totalCount should be(1)
    result2.results should be(Seq("KE115"))
  }

  test("That searching for grepcodes returns sensible results even if lowercase") {
    val Success(result) = grepCodesSearchService.matchingQuery("ke", 1, 100)

    result.totalCount should be(3)
    result.results should be(Seq("KE101", "KE105", "KE115"))

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
