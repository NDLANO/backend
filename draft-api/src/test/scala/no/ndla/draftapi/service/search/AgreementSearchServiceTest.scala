/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import no.ndla.draftapi.DraftApiProperties.DefaultPageSize
import no.ndla.draftapi.TestData.{agreementSearchSettings, searchSettings}
import no.ndla.draftapi._
import no.ndla.draftapi.integration.{Elastic4sClientFactory, NdlaE4sClient}
import no.ndla.draftapi.model.domain._
import no.ndla.scalatestsuite.IntegrationSuite
import org.joda.time.DateTime
import org.scalatest.Outcome

import scala.util.Success

class AgreementSearchServiceTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val agreementSearchService = new AgreementSearchService
  override val agreementIndexService = new AgreementIndexService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright(Some("by-nc-sa"),
                         Some("Gotham City"),
                         List(Author("Forfatter", "DC Comics")),
                         List(),
                         List(),
                         None,
                         None,
                         None)

  val publicDomain = Copyright(Some("publicdomain"),
                               Some("Metropolis"),
                               List(Author("Forfatter", "Bruce Wayne")),
                               List(),
                               List(),
                               None,
                               None,
                               None)

  val copyrighted = Copyright(Some("copyrighted"),
                              Some("New York"),
                              List(Author("Forfatter", "Clark Kent")),
                              List(),
                              List(),
                              None,
                              None,
                              None)

  val today: DateTime = DateTime.now()

  val sampleAgreement = new Agreement(
    Some(1),
    "title",
    "content",
    byNcSa,
    today.minusDays(2).toDate,
    today.minusDays(4).toDate,
    "ndla1234"
  )

  val agreement1: Agreement =
    sampleAgreement.copy(id = Some(2), title = "Aper får lov", content = "Aper får kjempe seg fremover")

  val agreement2: Agreement =
    sampleAgreement.copy(id = Some(3), title = "Ugler er slemme", content = "Ugler er de slemmeste dyrene")

  val agreement3: Agreement = sampleAgreement.copy(id = Some(4),
                                                   title = "Tyven stjeler penger",
                                                   content = "Det er ikke hemmelig at tyven er den som stjeler penger")

  val agreement4: Agreement =
    sampleAgreement.copy(id = Some(5), title = "Vi får låne bildene", content = "Vi får låne bildene av kjeltringene")

  val agreement5: Agreement =
    sampleAgreement.copy(id = Some(6), title = "Kjeltringer er ikke velkomne", content = "De er slemmere enn kjeft")

  val agreement6: Agreement =
    sampleAgreement.copy(id = Some(7), title = "Du er en tyv", content = "Det er du som er tyven")

  val agreement7: Agreement = sampleAgreement.copy(id = Some(8),
                                                   title = "Lurerier er ikke bra",
                                                   content = "Lurerier er bare lov dersom du er en tyv")

  val agreement8: Agreement =
    sampleAgreement.copy(id = Some(9), title = "Hvorfor er aper så slemme", content = "Har du blitt helt ape")

  val agreement9: Agreement =
    sampleAgreement.copy(id = Some(10), title = "Du er en av dem du", content = "Det er ikke snilt å være en av dem")

  val agreement10: Agreement =
    sampleAgreement.copy(id = Some(11), title = "Woopie", content = "This agreement is not copyrighted")

  override def beforeAll(): Unit = if (elasticSearchContainer.isSuccess) {
    agreementIndexService.createIndexWithName(DraftApiProperties.AgreementSearchIndex)

    agreementIndexService.indexDocument(agreement1)
    agreementIndexService.indexDocument(agreement2)
    agreementIndexService.indexDocument(agreement3)
    agreementIndexService.indexDocument(agreement4)
    agreementIndexService.indexDocument(agreement5)
    agreementIndexService.indexDocument(agreement6)
    agreementIndexService.indexDocument(agreement7)
    agreementIndexService.indexDocument(agreement8)
    agreementIndexService.indexDocument(agreement9)
    agreementIndexService.indexDocument(agreement10)

    blockUntil(() => {
      agreementSearchService.countDocuments == 10
    })
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    agreementSearchService.getStartAtAndNumResults(0, 10001) should equal((0, DraftApiProperties.MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    agreementSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal(
      (expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * DefaultPageSize
    agreementSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal(
      (expectedStartAt, DefaultPageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(results) = agreementSearchService.matchingQuery(agreementSearchSettings.copy(sort = Sort.ByIdAsc))
    val hits = results.results
    results.totalCount should be(10)
    hits.head.id should be(2)
    hits(1).id should be(3)
    hits(2).id should be(4)
    hits(3).id should be(5)
    hits(4).id should be(6)
    hits(5).id should be(7)
    hits(6).id should be(8)
    hits(7).id should be(9)
    hits(8).id should be(10)
    hits(9).id should be(11)
  }

  test("That all returns all documents ordered by id descending") {
    val Success(results) = agreementSearchService.matchingQuery(agreementSearchSettings.copy(sort = Sort.ByIdDesc))
    val hits = results.results
    results.totalCount should be(10)
    hits.head.id should be(11)
    hits.last.id should be(2)
  }

  test("That all returns all documents ordered by title ascending") {
    val Success(results) = agreementSearchService.matchingQuery(agreementSearchSettings.copy(sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(10)
    hits.head.id should be(2)
    hits(1).id should be(10)
    hits(2).id should be(7)
    hits(3).id should be(9)
    hits(4).id should be(6)
    hits(5).id should be(8)
    hits(6).id should be(4)
    hits(7).id should be(3)
    hits(8).id should be(5)
  }

  test("That all returns all documents ordered by title descending") {
    val Success(results) = agreementSearchService.matchingQuery(agreementSearchSettings.copy(sort = Sort.ByTitleDesc))
    val hits = results.results
    results.totalCount should be(10)
    hits.head.id should be(11)
    hits(1).id should be(5)
    hits(2).id should be(3)
    hits(3).id should be(4)
    hits(4).id should be(8)
    hits(5).id should be(6)
    hits(6).id should be(9)
    hits(7).id should be(7)
    hits(8).id should be(10)
    hits(9).id should be(2)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(page1) =
      agreementSearchService.matchingQuery(agreementSearchSettings.copy(page = 1, pageSize = 2, sort = Sort.ByTitleAsc))
    val hits1 = page1.results
    page1.totalCount should be(10)
    page1.page.get should be(1)
    hits1.size should be(2)
    hits1.head.id should be(2)
    hits1.last.id should be(10)

    val Success(page2) =
      agreementSearchService.matchingQuery(agreementSearchSettings.copy(page = 2, pageSize = 2, sort = Sort.ByTitleAsc))
    val hits2 = page2.results
    page2.totalCount should be(10)
    page2.page.get should be(2)
    hits2.size should be(2)
    hits2.head.id should be(7)
    hits2.last.id should be(9)
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(results) = agreementSearchService.matchingQuery(
      agreementSearchSettings.copy(
        query = Some("Du"),
        withIdIn = List(10),
        sort = Sort.ByRelevanceDesc
      ))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(10)
  }

  test("That search matches title") {
    val Success(results) = agreementSearchService.matchingQuery(
      agreementSearchSettings.copy(
        query = Some("Ugler"),
        sort = Sort.ByTitleAsc
      ))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val Success(results) = agreementSearchService.matchingQuery(
      agreementSearchSettings.copy(
        query = Some("supermann"),
        sort = Sort.ByTitleAsc
      ))
    results.totalCount should be(0)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) = agreementSearchService.matchingQuery(agreementSearchSettings.copy(query = Some("aper + du")))
    val hits1 = search1.results
    hits1.map(_.id) should equal(Seq(2, 7, 8, 9, 10))

    val Success(search2) =
      agreementSearchService.matchingQuery(agreementSearchSettings.copy(query = Some("lurerier + dersom")))
    val hits2 = search2.results
    hits2.map(_.id) should equal(Seq(8))

    val Success(search3) =
      agreementSearchService.matchingQuery(agreementSearchSettings.copy(query = Some("tyv + stjeler - Lurerier")))
    val hits3 = search3.results
    hits3.map(_.id) should equal(Seq(4, 7, 8))

    val Success(search4) =
      agreementSearchService.matchingQuery(agreementSearchSettings.copy(query = Some("aper -slemme")))
    val hits4 = search4.results
    hits4.map(_.id) should equal(Seq(2))
  }

  test("search in content should be ranked lower than title") {
    val Success(search) =
      agreementSearchService.matchingQuery(
        agreementSearchSettings.copy(query = Some("lov"), sort = Sort.ByRelevanceDesc))
    val hits = search.results
    hits.map(_.id) should equal(Seq(8, 2))
  }

  test("That scrolling works as expected") {
    val pageSize = 2
    val expectedIds = List(2, 3, 4, 5, 6, 7, 8, 9, 10, 11).sliding(pageSize, pageSize).toList

    val Success(initialSearch) =
      agreementSearchService.matchingQuery(agreementSearchSettings.copy(pageSize = pageSize, shouldScroll = true))

    val Success(scroll1) = agreementSearchService.scroll(initialSearch.scrollId.get, "*")
    val Success(scroll2) = agreementSearchService.scroll(scroll1.scrollId.get, "*")
    val Success(scroll3) = agreementSearchService.scroll(scroll2.scrollId.get, "*")
    val Success(scroll4) = agreementSearchService.scroll(scroll3.scrollId.get, "*")
    val Success(scroll5) = agreementSearchService.scroll(scroll4.scrollId.get, "*")

    initialSearch.results.map(_.id) should be(expectedIds.head)
    scroll1.results.map(_.id) should be(expectedIds(1))
    scroll2.results.map(_.id) should be(expectedIds(2))
    scroll3.results.map(_.id) should be(expectedIds(3))
    scroll4.results.map(_.id) should be(expectedIds(4))
    scroll5.results.map(_.id) should be(List.empty)
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
}
