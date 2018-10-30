/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import java.nio.file.{Files, Path}

import com.sksamuel.elastic4s.embedded.{InternalLocalNode, LocalNode}
import no.ndla.audioapi.integration.{Elastic4sClientFactory, NdlaE4sClient}
import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.{AudioApiProperties, TestEnvironment, UnitSuite}
import no.ndla.tag.IntegrationTest
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock

class SearchServiceTest extends UnitSuite with TestEnvironment {
  val tmpDir: Path = Files.createTempDirectory(this.getClass.getName)
  val localNodeSettings: Map[String, String] = LocalNode.requiredSettings(this.getClass.getName, tmpDir.toString)
  val localNode: InternalLocalNode = LocalNode(localNodeSettings)
  override val e4sClient: NdlaE4sClient = NdlaE4sClient(localNode.client(true))

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
    updated2
  )

  val audio2 = AudioMetaInformation(
    Some(2),
    Some(1),
    List(Title("Pingvinen er ute og går", "nb")),
    List(Audio("file2.mp3", "audio/mpeg", 1024, "nb")),
    publicDomain,
    List(Tag(List("fugl"), "nb")),
    "ndla124",
    updated4
  )

  val audio3 = AudioMetaInformation(
    Some(3),
    Some(1),
    List(Title("Superman er ute og flyr", "nb")),
    List(Audio("file4.mp3", "audio/mpeg", 1024, "nb")),
    byNcSa,
    List(Tag(List("supermann"), "nb")),
    "ndla124",
    updated3
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
    updated5
  )

  val audio5 = AudioMetaInformation(
    Some(5),
    Some(1),
    List(Title("Synge sangen", "nb")),
    List(Audio("file5.mp3", "audio/mpeg", 1024, "nb")),
    byNcSa.copy(agreementId = Some(1)),
    List(Tag(List("synge"), "nb")),
    "ndla124",
    updated1
  )

  val audio6 = AudioMetaInformation(
    Some(6),
    Some(1),
    List(Title("Urelatert", "nb"), Title("Unrelated", "en")),
    List(Audio("en.mp3", "audio/mpeg", 1024, "en"), Audio("nb.mp3", "audio/mpeg", 1024, "nb")),
    byNcSa,
    List(Tag(List("wubbi"), "nb"), Tag(List("knakki"), "en")),
    "ndla123",
    updated6
  )

  override def beforeAll: Unit = {
    when(converterService.withAgreementCopyright(any[AudioMetaInformation])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[AudioMetaInformation](0))
    when(converterService.withAgreementCopyright(audio5))
      .thenReturn(audio5.copy(copyright = audio5.copyright.copy(license = "gnu")))

    indexService.createIndexWithName(AudioApiProperties.SearchIndex)

    indexService.indexDocument(audio1)
    indexService.indexDocument(audio2)
    indexService.indexDocument(audio3)
    indexService.indexDocument(audio4)
    indexService.indexDocument(audio5)
    indexService.indexDocument(audio6)

    blockUntil(() => searchService.countDocuments == 6)
  }

  override def afterAll(): Unit = {
    indexService.deleteIndexWithName(Some(AudioApiProperties.SearchIndex))
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService.getStartAtAndNumResults(None, None) should equal((0, AudioApiProperties.DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService.getStartAtAndNumResults(None, Some(1000)) should equal((0, AudioApiProperties.MaxPageSize))
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
    val results = searchService.all(None, None, None, None, Sort.ByTitleAsc)
    results.totalCount should be(5)
    results.results.head.id should be(4)
    results.results.last.id should be(6)
  }

  test("That filtering on license only returns documents with given license for all languages") {
    val results = searchService.all(None, Some("publicdomain"), None, None, Sort.ByTitleAsc)
    results.totalCount should be(2)
    results.results.head.id should be(4)
    results.results.last.id should be(2)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = searchService.all(None, None, Some(1), Some(2), Sort.ByTitleAsc)
    val page2 = searchService.all(None, None, Some(2), Some(2), Sort.ByTitleAsc)
    page1.totalCount should be(5)
    page1.page should be(1)
    page1.results.size should be(2)
    page1.results.head.id should be(4)
    page1.results.last.id should be(2)
    page2.totalCount should be(5)
    page2.page should be(2)
    page2.results.size should be(2)
    page2.results.head.id should be(3)
  }

  test("That search matches title") {
    val results = searchService.matchingQuery("Pingvinen", Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be(1)
    results.results.head.id should be(2)
  }

  test("That search matches tags") {
    val results = searchService.matchingQuery("and", Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be(1)
    results.results.head.id should be(4)
  }

  test("That search does not return batmen since it has license copyrighted and license is not specified") {
    val results = searchService.matchingQuery("batmen", Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be(0)
  }

  test("That search returns batmen since license is specified as copyrighted") {
    val results = searchService.matchingQuery("batmen", Some("nb"), Some("copyrighted"), None, None, Sort.ByTitleAsc)
    results.totalCount should be(1)
    results.results.head.id should be(1)
  }

  test("Searching with logical AND only returns results with all terms") {
    val search1 = searchService.matchingQuery("bilde + bil", Some("nb"), None, None, None, Sort.ByTitleAsc)
    search1.results.map(_.id) should equal(Seq.empty)

    val search2 = searchService.matchingQuery("ute + -går", Some("nb"), None, None, None, Sort.ByTitleAsc)
    search2.results.map(_.id) should equal(Seq(3))
  }

  test("That searching for all languages and specifying no language should return the same") {
    val results1 = searchService.all(None, None, None, None, Sort.ByTitleAsc)
    val results2 = searchService.all(None, None, None, None, Sort.ByTitleAsc)

    results1.totalCount should be(results2.totalCount)
    results1.results(0) should be(results2.results(0))
    results1.results(1) should be(results2.results(1))
    results1.results(2) should be(results2.results(2))
  }

  test("That searching for 'nb' should return all results") {
    val results = searchService.all(Some("nb"), None, None, None, Sort.ByTitleAsc)
    results.totalCount should be(5)
  }

  test("That searching for 'en' should only return results with english title") {
    val result = searchService.all(Some("en"), None, None, None, Sort.ByTitleAsc)
    result.totalCount should be(2)
    result.language should be("en")

    result.results.head.title.title should be("Donald Duck drives a car")
    result.results.head.title.language should be("en")

    result.results.last.title.title should be("Unrelated")
    result.results.last.title.language should be("en")
  }

  test("That 'supported languages' should match all possible title languages") {
    val result1 = searchService.all(Some("en"), None, None, None, Sort.ByTitleAsc)
    val result2 = searchService.all(Some("nb"), None, None, None, Sort.ByTitleAsc)

    // 'Donald' with 'en', 'nb' and 'nn'
    result1.results.head.supportedLanguages should be(audio4.titles.map(_.language))
    // 'Pingvinen' with 'nb'
    result2.results(2).supportedLanguages should be(audio1.titles.map(_.language))
  }

  test("Agreement information should be used in search") {
    val searchResult = searchService.matchingQuery("Synge sangen", None, None, None, None, Sort.ByTitleAsc)
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
    val searchResultEn = searchService.matchingQuery("Unrelated", None, None, None, None, Sort.ByTitleAsc)
    val searchResultNb = searchService.matchingQuery("Urelatert", None, None, None, None, Sort.ByTitleAsc)

    searchResultNb.totalCount should be(1)
    searchResultNb.results.head.title.language should be("nb")
    searchResultNb.results.head.title.title should be("Urelatert")

    searchResultEn.totalCount should be(1)
    searchResultEn.results.head.title.language should be("en")
    searchResultEn.results.head.title.title should be("Unrelated")
  }

  test("That sorting by lastUpdated asc functions correctly") {
    val search = searchService.all(None, None, None, None, Sort.ByLastUpdatedAsc)

    search.totalCount should be(5)
    search.results(0).id should be(5)
    search.results(1).id should be(3)
    search.results(2).id should be(2)
    search.results(3).id should be(4)
    search.results(4).id should be(6)
  }

  test("That sorting by lastUpdated desc functions correctly") {
    val search = searchService.all(None, None, None, None, Sort.ByLastUpdatedDesc)

    search.totalCount should be(5)
    search.results(0).id should be(6)
    search.results(1).id should be(4)
    search.results(2).id should be(2)
    search.results(3).id should be(3)
    search.results(4).id should be(5)
  }

  test("That sorting by id asc functions correctly") {
    val search = searchService.all(None, None, None, None, Sort.ByIdAsc)

    search.totalCount should be(5)
    search.results(0).id should be(2)
    search.results(1).id should be(3)
    search.results(2).id should be(4)
    search.results(3).id should be(5)
    search.results(4).id should be(6)
  }

  test("That sorting by id desc functions correctly") {
    val search = searchService.all(None, None, None, None, Sort.ByIdDesc)

    search.totalCount should be(5)
    search.results(0).id should be(6)
    search.results(1).id should be(5)
    search.results(2).id should be(4)
    search.results(3).id should be(3)
    search.results(4).id should be(2)
  }

  test("That supportedLanguages are sorted correctly") {
    val result = searchService.matchingQuery("Unrelated", None, None, None, None, Sort.ByTitleAsc)
    result.results.head.supportedLanguages should be(Seq("nb", "en"))
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
