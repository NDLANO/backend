/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import no.ndla.audioapi.integration.{Elastic4sClientFactory, NdlaE4sClient}
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.{AudioApiProperties, TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.IntegrationSuite
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.Outcome
import org.testcontainers.elasticsearch.ElasticsearchContainer

import scala.util.{Success, Try}

class SearchServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {
  override val e4sClient: NdlaE4sClient =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override val searchService = new SearchService
  override val indexService = new IndexService
  override val searchConverterService = new SearchConverterService

  val byNcSa =
    Copyright("by-nc-sa", Some("Gotham City"), List(Author("Forfatter", "DC Comics")), Seq(), Seq(), None, None, None)

  val publicDomain = Copyright("publicdomain",
                               Some("Metropolis"),
                               List(Author("Forfatter", "Bruce Wayne")),
                               Seq(),
                               Seq(),
                               None,
                               None,
                               None)

  val copyrighted =
    Copyright("copyrighted", Some("New York"), List(Author("Forfatter", "Clark Kent")), Seq(), Seq(), None, None, None)

  val updated1 = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate
  val updated2 = new DateTime(2017, 5, 1, 12, 15, 32, DateTimeZone.UTC).toDate
  val updated3 = new DateTime(2017, 6, 1, 12, 15, 32, DateTimeZone.UTC).toDate
  val updated4 = new DateTime(2017, 7, 1, 12, 15, 32, DateTimeZone.UTC).toDate
  val updated5 = new DateTime(2017, 8, 1, 12, 15, 32, DateTimeZone.UTC).toDate
  val updated6 = new DateTime(2017, 9, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val audio1 = AudioMetaInformation(
    Some(1),
    Some(1),
    List(Title("Batmen er på vift med en bil", "nb")),
    List(Audio("file.mp3", "audio/mpeg", 1024, "nb")),
    copyrighted,
    List(Tag(List("fisk"), "nb")),
    "ndla124",
    updated2,
    Seq.empty,
    AudioType.Standard
  )

  val audio2 = AudioMetaInformation(
    Some(2),
    Some(1),
    List(Title("Pingvinen er ute og går", "nb")),
    List(Audio("file2.mp3", "audio/mpeg", 1024, "nb")),
    publicDomain,
    List(Tag(List("fugl"), "nb")),
    "ndla124",
    updated4,
    Seq.empty,
    AudioType.Standard
  )

  val audio3 = AudioMetaInformation(
    Some(3),
    Some(1),
    List(Title("Superman er ute og flyr", "nb")),
    List(Audio("file4.mp3", "audio/mpeg", 1024, "nb")),
    byNcSa,
    List(Tag(List("supermann"), "nb")),
    "ndla124",
    updated3,
    Seq.empty,
    AudioType.Standard
  )

  val audio4 = AudioMetaInformation(
    Some(4),
    Some(1),
    List(Title("Donald Duck kjører bil", "nb"),
         Title("Donald Duck kjører bil", "nn"),
         Title("Donald Duck drives a car", "en")),
    List(Audio("file3.mp3", "audio/mpeg", 1024, "nb")),
    publicDomain,
    List(Tag(List("and"), "nb")),
    "ndla124",
    updated5,
    Seq.empty,
    AudioType.Standard
  )

  val audio5 = AudioMetaInformation(
    Some(5),
    Some(1),
    List(Title("Synge sangen", "nb")),
    List(Audio("file5.mp3", "audio/mpeg", 1024, "nb")),
    byNcSa.copy(agreementId = Some(1)),
    List(Tag(List("synge"), "nb")),
    "ndla124",
    updated1,
    Seq.empty,
    AudioType.Standard
  )

  val audio6 = AudioMetaInformation(
    Some(6),
    Some(1),
    List(Title("Urelatert", "nb"), Title("Unrelated", "en")),
    List(Audio("en.mp3", "audio/mpeg", 1024, "en"), Audio("nb.mp3", "audio/mpeg", 1024, "nb")),
    byNcSa,
    List(Tag(List("wubbi"), "nb"), Tag(List("knakki"), "en")),
    "ndla123",
    updated6,
    Seq.empty,
    AudioType.Standard
  )

  val searchSettings: SearchSettings = SearchSettings(
    query = None,
    language = None,
    license = None,
    page = None,
    pageSize = None,
    sort = Sort.ByTitleAsc,
    shouldScroll = false
  )

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(converterService.withAgreementCopyright(any[AudioMetaInformation])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[AudioMetaInformation](0))
    when(converterService.withAgreementCopyright(audio5))
      .thenReturn(audio5.copy(copyright = audio5.copyright.copy(license = "gnu")))

    if (elasticSearchContainer.isSuccess) {
      indexService.createIndexWithName(AudioApiProperties.SearchIndex)
      indexService.indexDocument(audio1)
      indexService.indexDocument(audio2)
      indexService.indexDocument(audio3)
      indexService.indexDocument(audio4)
      indexService.indexDocument(audio5)
      indexService.indexDocument(audio6)

      blockUntil(() => searchService.countDocuments == 6)
    }
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService.getStartAtAndNumResults(None, None) should equal((0, AudioApiProperties.DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService.getStartAtAndNumResults(None, Some(10001)) should equal((0, AudioApiProperties.MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * AudioApiProperties.DefaultPageSize
    searchService.getStartAtAndNumResults(Some(page), None) should equal(
      (expectedStartAt, AudioApiProperties.DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * AudioApiProperties.MaxPageSize
    searchService.getStartAtAndNumResults(Some(page), Some(AudioApiProperties.MaxPageSize)) should equal(
      (expectedStartAt, AudioApiProperties.MaxPageSize))
  }

  test("That no language returns all documents ordered by title ascending") {
    val Success(results) = searchService.matchingQuery(searchSettings.copy())
    results.totalCount should be(5)
    results.results.head.id should be(4)
    results.results.last.id should be(6)
  }

  test("That filtering on license only returns documents with given license for all languages") {
    val Success(results) = searchService.matchingQuery(searchSettings.copy(license = Some("publicdomain")))
    results.totalCount should be(2)
    results.results.head.id should be(4)
    results.results.last.id should be(2)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(page1) = searchService.matchingQuery(searchSettings.copy(page = Some(1), pageSize = Some(2)))
    val Success(page2) = searchService.matchingQuery(searchSettings.copy(page = Some(2), pageSize = Some(2)))
    page1.totalCount should be(5)
    page1.page.get should be(1)
    page1.results.size should be(2)
    page1.results.head.id should be(4)
    page1.results.last.id should be(2)
    page2.totalCount should be(5)
    page2.page.get should be(2)
    page2.results.size should be(2)
    page2.results.head.id should be(3)
  }

  test("That search matches title") {
    val Success(results) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("Pingvinen"),
        language = Some("nb")
      ))
    results.totalCount should be(1)
    results.results.head.id should be(2)
  }

  test("That search matches id") {
    val Success(results) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("2"),
        language = Some("nb")
      ))
    results.totalCount should be(1)
    results.results.head.id should be(2)
  }

  test("That search matches tags") {
    val Success(results) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("and"),
        language = Some("nb")
      ))
    results.totalCount should be(1)
    results.results.head.id should be(4)
  }

  test("That search does not return batmen since it has license copyrighted and license is not specified") {
    val Success(results) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("batmen"),
        language = Some("nb")
      ))
    results.totalCount should be(0)
  }

  test("That search returns batmen since license is specified as copyrighted") {
    val Success(results) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("batmen"),
        language = Some("nb"),
        license = Some("copyrighted")
      ))
    results.totalCount should be(1)
    results.results.head.id should be(1)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("bilde + bil"),
        language = Some("nb"),
      ))
    search1.results.map(_.id) should equal(Seq.empty)

    val Success(search2) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("ute + -går"),
        language = Some("nb"),
      ))
    search2.results.map(_.id) should equal(Seq(3))
  }

  test("That searching for all languages and specifying no language should return the same") {
    val Success(results1) = searchService.matchingQuery(searchSettings.copy(language = Some("all")))
    val Success(results2) = searchService.matchingQuery(searchSettings.copy(language = None))

    results1.totalCount should be(results2.totalCount)
    results1.results.head should be(results2.results.head)
    results1.results(1) should be(results2.results(1))
    results1.results(2) should be(results2.results(2))
  }

  test("That searching for 'nb' should return all results") {
    val Success(results) = searchService.matchingQuery(searchSettings.copy(language = Some("nb")))
    results.totalCount should be(5)
  }

  test("That searching for 'en' should only return results with english title") {
    val Success(result) = searchService.matchingQuery(searchSettings.copy(language = Some("en")))
    result.totalCount should be(2)
    result.language should be("en")

    result.results.head.title.title should be("Donald Duck drives a car")
    result.results.head.title.language should be("en")

    result.results.last.title.title should be("Unrelated")
    result.results.last.title.language should be("en")
  }

  test("That 'supported languages' should match all possible title languages") {
    val Success(result1) = searchService.matchingQuery(searchSettings.copy(language = Some("en")))
    val Success(result2) = searchService.matchingQuery(searchSettings.copy(language = Some("nb")))

    // 'Donald' with 'en', 'nb' and 'nn'
    result1.results.head.supportedLanguages should be(audio4.titles.map(_.language))
    // 'Pingvinen' with 'nb'
    result2.results(2).supportedLanguages should be(audio1.titles.map(_.language))
  }

  test("Agreement information should be used in search") {
    val Success(searchResult) = searchService.matchingQuery(searchSettings.copy(query = Some("Synge sangen")))
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be(5)
    searchResult.results.head.license should equal("gnu")
  }

  test("that hit is converted to summary correctly") {
    val id = 5
    val title = "Synge sangen"
    val license = "gnu"
    val tag = "synge"
    val supportedLanguages = Seq("nb")
    val hitString =
      s"""{"tags":{"nb":["$tag"]},"license":"$license","titles":{"nb":"$title"},"id":"$id","authors":["DC Comics"]}"""

    val result = searchService.hitAsAudioSummary(hitString, "nb")

    result.id should equal(id)
    result.title.title should equal(title)
    result.license should equal(license)
    result.supportedLanguages should equal(supportedLanguages)
  }

  test("That hit is returned in the matched language") {
    val Success(searchResultEn) = searchService.matchingQuery(searchSettings.copy(query = Some("Unrelated")))
    val Success(searchResultNb) = searchService.matchingQuery(searchSettings.copy(query = Some("Urelatert")))

    searchResultNb.totalCount should be(1)
    searchResultNb.results.head.title.language should be("nb")
    searchResultNb.results.head.title.title should be("Urelatert")

    searchResultEn.totalCount should be(1)
    searchResultEn.results.head.title.language should be("en")
    searchResultEn.results.head.title.title should be("Unrelated")
  }

  test("That sorting by lastUpdated asc functions correctly") {
    val Success(search) = searchService.matchingQuery(searchSettings.copy(sort = Sort.ByLastUpdatedAsc))

    search.totalCount should be(5)
    search.results.head.id should be(5)
    search.results(1).id should be(3)
    search.results(2).id should be(2)
    search.results(3).id should be(4)
    search.results(4).id should be(6)
  }

  test("That sorting by lastUpdated desc functions correctly") {
    val Success(search) = searchService.matchingQuery(searchSettings.copy(sort = Sort.ByLastUpdatedDesc))

    search.totalCount should be(5)
    search.results.head.id should be(6)
    search.results(1).id should be(4)
    search.results(2).id should be(2)
    search.results(3).id should be(3)
    search.results(4).id should be(5)
  }

  test("That sorting by id asc functions correctly") {
    val Success(search) = searchService.matchingQuery(searchSettings.copy(sort = Sort.ByIdAsc))

    search.totalCount should be(5)
    search.results.head.id should be(2)
    search.results(1).id should be(3)
    search.results(2).id should be(4)
    search.results(3).id should be(5)
    search.results(4).id should be(6)
  }

  test("That sorting by id desc functions correctly") {
    val Success(search) = searchService.matchingQuery(searchSettings.copy(sort = Sort.ByIdDesc))

    search.totalCount should be(5)
    search.results.head.id should be(6)
    search.results(1).id should be(5)
    search.results(2).id should be(4)
    search.results(3).id should be(3)
    search.results(4).id should be(2)
  }

  test("That supportedLanguages are sorted correctly") {
    val Success(result) = searchService.matchingQuery(searchSettings.copy(query = Some("Unrelated")))
    result.results.head.supportedLanguages should be(Seq("nb", "en"))
  }

  test("That scrolling works as expected") {
    val pageSize = 2
    val expectedIds = List(2, 3, 4, 5, 6).sliding(pageSize, pageSize).toList

    val Success(initialSearch) =
      searchService.matchingQuery(
        searchSettings.copy(pageSize = Some(pageSize), sort = Sort.ByIdAsc, shouldScroll = true))

    val Success(scroll1) = searchService.scroll(initialSearch.scrollId.get, "all")
    val Success(scroll2) = searchService.scroll(scroll1.scrollId.get, "all")
    val Success(scroll3) = searchService.scroll(scroll2.scrollId.get, "all")

    initialSearch.results.map(_.id) should be(expectedIds.head)
    scroll1.results.map(_.id) should be(expectedIds(1))
    scroll2.results.map(_.id) should be(expectedIds(2))
    scroll3.results.map(_.id) should be(List.empty)
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
