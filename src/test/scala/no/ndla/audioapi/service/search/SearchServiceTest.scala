/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.audioapi.service.search

import no.ndla.audioapi.integration.JestClientFactory
import no.ndla.audioapi.model.{Language, Sort}
import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.{AudioApiProperties, TestEnvironment, UnitSuite}
import no.ndla.tag.IntegrationTest
import org.joda.time.{DateTime, DateTimeZone}

@IntegrationTest
class SearchServiceTest extends UnitSuite with TestEnvironment {

  val esPort = 9200

  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esPort")


  override val searchService = new SearchService
  override val indexService = new IndexService
  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright("by-nc-sa", Some("Gotham City"), List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright("publicdomain", Some("Metropolis"), List(Author("Forfatter", "Bruce Wayne")))
  val copyrighted = Copyright("copyrighted", Some("New York"), List(Author("Forfatter", "Clark Kent")))
  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val audio1 = AudioMetaInformation(Some(1), List(Title("Batmen er på vift med en bil", Some("nb"))), List(Audio("file.mp3", "audio/mpeg", 1024, Some("nb"))), copyrighted, List(Tag(List("fisk"), Some("nb"))), "ndla124", updated)
  val audio2 = AudioMetaInformation(Some(2), List(Title("Pingvinen er ute og går", Some("nb"))), List(Audio("file2.mp3", "audio/mpeg", 1024, Some("nb"))), publicDomain, List(Tag(List("fugl"), Some("nb"))), "ndla124", updated)
  val audio3 = AudioMetaInformation(Some(3), List(Title("Superman er ute og flyr", Some("nb"))), List(Audio("file4.mp3", "audio/mpeg", 1024, Some("nb"))), byNcSa, List(Tag(List("supermann"), Some("nb"))), "ndla124", updated)
  val audio4 = AudioMetaInformation(Some(4), List(Title("Donald Duck kjører bil", Some("nb")), Title("Donald Duck kjører bil", Some("nn")), Title("Donald Duck drives a car", Some("en"))), List(Audio("file3.mp3", "audio/mpeg", 1024, Some("nb"))), publicDomain, List(Tag(List("and"), Some("nb"))), "ndla124", updated)

  override def beforeAll = {
    indexService.createIndexWithName(AudioApiProperties.SearchIndex)

    indexService.indexDocument(audio1)
    indexService.indexDocument(audio2)
    indexService.indexDocument(audio3)
    indexService.indexDocument(audio4)

    blockUntil(() => searchService.countDocuments() == 4)
  }

  override def afterAll() = {
    indexService.delete(Some(AudioApiProperties.SearchIndex))
  }


  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService.getStartAtAndNumResults(None, None) should equal((0, AudioApiProperties.DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService.getStartAtAndNumResults(None, Some(1000)) should equal((0, AudioApiProperties.MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * AudioApiProperties.DefaultPageSize
    searchService.getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, AudioApiProperties.DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * AudioApiProperties.MaxPageSize
    searchService.getStartAtAndNumResults(Some(page), Some(AudioApiProperties.MaxPageSize)) should equal((expectedStartAt, AudioApiProperties.MaxPageSize))
  }

  test("That all returns all documents ordered by title ascending") {
    val results = searchService.all(Language.AllLanguages, None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (3)
    results.results.head.id should be (4)
    results.results.last.id should be (3)
  }

  test("That all filtering on license only returns documents with given license") {
    val results = searchService.all(Language.AllLanguages, Some("publicdomain"), None, None, Sort.ByTitleAsc)
    results.totalCount should be (2)
    results.results.head.id should be (4)
    results.results.last.id should be (2)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = searchService.all(Language.AllLanguages, None, Some(1), Some(2), Sort.ByTitleAsc)
    val page2 = searchService.all(Language.AllLanguages, None, Some(2), Some(2), Sort.ByTitleAsc)
    page1.totalCount should be (3)
    page1.page should be (1)
    page1.results.size should be (2)
    page1.results.head.id should be (4)
    page1.results.last.id should be (2)
    page2.totalCount should be (3)
    page2.page should be (2)
    page2.results.size should be (1)
    page2.results.head.id should be (3)
  }

  test("That search matches title") {
    val results = searchService.matchingQuery("Pingvinen", "nb", None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (1)
    results.results.head.id should be (2)
  }

  test("That search matches tags") {
    val results = searchService.matchingQuery("and", "nb", None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (1)
    results.results.head.id should be (4)
  }

  test("That search does not return batmen since it has license copyrighted and license is not specified") {
    val results = searchService.matchingQuery("batmen", "nb", None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (0)
  }

  test("That search returns batmen since license is specified as copyrighted") {
    val results = searchService.matchingQuery("batmen", "nb", Some("copyrighted"), None, None, Sort.ByTitleAsc)
    results.totalCount should be (1)
    results.results.head.id should be (1)
  }

  test("Searching with logical AND only returns results with all terms") {
    val search1 = searchService.matchingQuery("bilde + bil", "nb", None, None, None, Sort.ByTitleAsc)
    search1.results.map(_.id) should equal (Seq.empty)

    val search2 = searchService.matchingQuery("ute + -går", "nb", None, None, None, Sort.ByTitleAsc)
    search2.results.map(_.id) should equal (Seq(3))
  }

  test("That searching for all languages and specifying no language should return the same") {
    val results1 = searchService.all(Language.AllLanguages, None, None, None, Sort.ByTitleAsc)
    val results2 = searchService.all(Language.AllLanguages, None, None, None, Sort.ByTitleAsc)

    results1.totalCount should be (results2.totalCount)
    results1.results(0) should be (results2.results(0))
    results1.results(1) should be (results2.results(1))
    results1.results(2) should be (results2.results(2))
  }

  test("That searching for 'nb' should return all results") {
    val results = searchService.all("nb", None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (3)
  }

  test("That searching for 'en' should only return 'Donald' (audio4) with the english title") {
    val result = searchService.all("en", None, None, None, Sort.ByTitleAsc)
    result.totalCount should be (1)
    result.results.head.title should be ("Donald Duck drives a car")
    result.language should be ("en")
  }

  test("That 'supported languages' should match all possible title languages") {
    val result1 = searchService.all("en", None, None, None, Sort.ByTitleAsc)
    val result2 = searchService.all("nb", None, None, None, Sort.ByTitleAsc)

    // 'Donald' with 'en', 'nb' and 'nn'
    result1.results.head.supportedLanguages should be (audio4.titles.map(_.language.getOrElse("")))
    // 'Pingvinen' with 'nb'
    result2.results(2).supportedLanguages should be (audio1.titles.map(_.language.getOrElse("")))
  }

  def blockUntil(predicate: () => Boolean) = {
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
