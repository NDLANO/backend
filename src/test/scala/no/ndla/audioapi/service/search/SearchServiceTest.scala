/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.audioapi.service.search

import no.ndla.audioapi.integration.{Elastic4sClientFactory, JestClientFactory}
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.{AudioApiProperties, TestEnvironment, UnitSuite}
import no.ndla.tag.IntegrationTest
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock

@IntegrationTest
class SearchServiceTest extends UnitSuite with TestEnvironment {

  val esPort = 9200

  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esPort")
  override val e4sClient = Elastic4sClientFactory.getClient(searchServer = s"http://localhost:$esPort")


  override val searchService = new SearchService
  override val indexService = new IndexService
  override val searchConverterService = new SearchConverterService

  val byNcSa = Copyright("by-nc-sa", Some("Gotham City"), List(Author("Forfatter", "DC Comics")), Seq(), Seq(), None, None, None)
  val publicDomain = Copyright("publicdomain", Some("Metropolis"), List(Author("Forfatter", "Bruce Wayne")), Seq(), Seq(), None, None, None)
  val copyrighted = Copyright("copyrighted", Some("New York"), List(Author("Forfatter", "Clark Kent")), Seq(), Seq(), None, None, None)
  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val audio1 = AudioMetaInformation(Some(1), Some(1), List(Title("Batmen er på vift med en bil", "nb")), List(Audio("file.mp3", "audio/mpeg", 1024, "nb")), copyrighted, List(Tag(List("fisk"), "nb")), "ndla124", updated)
  val audio2 = AudioMetaInformation(Some(2), Some(1), List(Title("Pingvinen er ute og går", "nb")), List(Audio("file2.mp3", "audio/mpeg", 1024, "nb")), publicDomain, List(Tag(List("fugl"), "nb")), "ndla124", updated)
  val audio3 = AudioMetaInformation(Some(3), Some(1), List(Title("Superman er ute og flyr", "nb")), List(Audio("file4.mp3", "audio/mpeg", 1024, "nb")), byNcSa, List(Tag(List("supermann"), "nb")), "ndla124", updated)
  val audio4 = AudioMetaInformation(Some(4), Some(1), List(Title("Donald Duck kjører bil", "nb"), Title("Donald Duck kjører bil", "nn"), Title("Donald Duck drives a car", "en")), List(Audio("file3.mp3", "audio/mpeg", 1024, "nb")), publicDomain, List(Tag(List("and"), "nb")), "ndla124", updated)
  val audio5 = AudioMetaInformation(Some(5), Some(1), List(Title("Synge sangen", "nb")), List(Audio("file5.mp3", "audio/mpeg", 1024, "nb")), byNcSa.copy(agreementId = Some(1)), List(Tag(List("synge"), "nb")), "ndla124", updated)

  override def beforeAll = {
    when(converterService.withAgreementCopyright(any[AudioMetaInformation])).thenAnswer((i: InvocationOnMock) =>
      i.getArgumentAt(0, audio1.getClass))
    when(converterService.withAgreementCopyright(audio5)).thenReturn(audio5.copy(copyright = audio5.copyright.copy(license = "gnu")))

    indexService.createIndexWithName(AudioApiProperties.SearchIndex)

    indexService.indexDocument(audio1)
    indexService.indexDocument(audio2)
    indexService.indexDocument(audio3)
    indexService.indexDocument(audio4)
    indexService.indexDocument(audio5)

    blockUntil(() => searchService.countDocuments == 5)
  }

  override def afterAll() = {
    indexService.deleteIndexWithName(Some(AudioApiProperties.SearchIndex))
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

  test("That no language returns all documents ordered by title ascending") {
    val results = searchService.all(None, None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (4)
    results.results.head.id should be (4)
    results.results.last.id should be (5)
  }

  test("That filtering on license only returns documents with given license for all languages") {
    val results = searchService.all(None, Some("publicdomain"), None, None, Sort.ByTitleAsc)
    results.totalCount should be (2)
    results.results.head.id should be (4)
    results.results.last.id should be (2)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = searchService.all(None, None, Some(1), Some(2), Sort.ByTitleAsc)
    val page2 = searchService.all(None, None, Some(2), Some(2), Sort.ByTitleAsc)
    page1.totalCount should be (4)
    page1.page should be (1)
    page1.results.size should be (2)
    page1.results.head.id should be (4)
    page1.results.last.id should be (2)
    page2.totalCount should be (4)
    page2.page should be (2)
    page2.results.size should be (2)
    page2.results.head.id should be (3)
  }

  test("That search matches title") {
    val results = searchService.matchingQuery("Pingvinen", Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (1)
    results.results.head.id should be (2)
  }

  test("That search matches tags") {
    val results = searchService.matchingQuery("and", Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (1)
    results.results.head.id should be (4)
  }

  test("That search does not return batmen since it has license copyrighted and license is not specified") {
    val results = searchService.matchingQuery("batmen", Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (0)
  }

  test("That search returns batmen since license is specified as copyrighted") {
    val results = searchService.matchingQuery("batmen", Some("nb"), Some("copyrighted"), None, None, Sort.ByTitleAsc)
    results.totalCount should be (1)
    results.results.head.id should be (1)
  }

  test("Searching with logical AND only returns results with all terms") {
    val search1 = searchService.matchingQuery("bilde + bil", Some("nb"), None, None, None, Sort.ByTitleAsc)
    search1.results.map(_.id) should equal (Seq.empty)

    val search2 = searchService.matchingQuery("ute + -går", Some("nb"), None, None, None, Sort.ByTitleAsc)
    search2.results.map(_.id) should equal (Seq(3))
  }

  test("That searching for all languages and specifying no language should return the same") {
    val results1 = searchService.all(None, None, None, None, Sort.ByTitleAsc)
    val results2 = searchService.all(None, None, None, None, Sort.ByTitleAsc)

    results1.totalCount should be (results2.totalCount)
    results1.results(0) should be (results2.results(0))
    results1.results(1) should be (results2.results(1))
    results1.results(2) should be (results2.results(2))
  }

  test("That searching for 'nb' should return all results") {
    val results = searchService.all(Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be (4)
  }

  test("That searching for 'en' should only return 'Donald' (audio4) with the english title") {
    val result = searchService.all(Some("en"), None, None, None, Sort.ByTitleAsc)
    result.totalCount should be (1)
    result.results.head.title.title should be ("Donald Duck drives a car")
    result.language should be ("en")
  }

  test("That 'supported languages' should match all possible title languages") {
    val result1 = searchService.all(Some("en"), None, None, None, Sort.ByTitleAsc)
    val result2 = searchService.all(Some("nb"), None, None, None, Sort.ByTitleAsc)

    // 'Donald' with 'en', 'nb' and 'nn'
    result1.results.head.supportedLanguages should be (audio4.titles.map(_.language))
    // 'Pingvinen' with 'nb'
    result2.results(2).supportedLanguages should be (audio1.titles.map(_.language))
  }

  test("Agreement information should be used in search") {
    val searchResult = searchService.matchingQuery("Synge sangen", None, None, None, None, Sort.ByTitleAsc)
    searchResult.totalCount should be (1)
    searchResult.results.size should be (1)
    searchResult.results.head.id should be (5)
    searchResult.results.head.license should equal("gnu")
  }

  test("that hit is converted to summary correctly") {
    val id = 5
    val title = "Synge sangen"
    val license = "gnu"
    val tag = "synge"
    val supportedLanguages = Set("nb")
    val hitString = s"""{"tags":{"nb":["$tag"]},"license":"$license","titles":{"nb":"$title"},"id":"$id","authors":["DC Comics"]}"""

    val result = searchService.hitAsAudioSummary(hitString, "nb")

    result.id should equal(id)
    result.title.title should equal(title)
    result.license should equal(license)
    result.supportedLanguages.toSet should equal(supportedLanguages)
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
