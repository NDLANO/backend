/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import no.ndla.conceptapi.ConceptApiProperties.{DefaultLanguage, DefaultPageSize}
import no.ndla.conceptapi.integration.Elastic4sClientFactory
import no.ndla.conceptapi.model.api.SubjectTags
import no.ndla.conceptapi.model.domain._
import no.ndla.conceptapi.model.search.SearchSettings
import no.ndla.conceptapi.{TestEnvironment, _}
import no.ndla.scalatestsuite.IntegrationSuite
import org.joda.time.DateTime
import org.scalatest.Outcome

import scala.util.Success

class PublishedConceptSearchServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with TestEnvironment {
  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val publishedConceptSearchService = new PublishedConceptSearchService
  override val publishedConceptIndexService = new PublishedConceptIndexService
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

  val concept1: Concept = TestData.sampleConcept.copy(
    id = Option(1),
    title = List(ConceptTitle("Batmen er på vift med en bil", "nb")),
    content =
      List(ConceptContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb"))
  )

  val concept2: Concept = TestData.sampleConcept.copy(
    id = Option(2),
    title = List(ConceptTitle("pingvinen er ute og går", "nb")),
    content = List(ConceptContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb"))
  )

  val concept3: Concept = TestData.sampleConcept.copy(
    id = Option(3),
    title = List(ConceptTitle("Donald Duck kjører bil", "nb")),
    content = List(ConceptContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb"))
  )

  val concept4: Concept = TestData.sampleConcept.copy(
    id = Option(4),
    title = List(ConceptTitle("Superman er ute og flyr", "nb")),
    content =
      List(ConceptContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb"))
  )

  val concept5: Concept = TestData.sampleConcept.copy(
    id = Option(5),
    title = List(ConceptTitle("Hulken løfter biler", "nb")),
    content = List(ConceptContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb"))
  )

  val concept6: Concept = TestData.sampleConcept.copy(
    id = Option(6),
    title = List(ConceptTitle("Loke og Tor prøver å fange midgaardsormen", "nb")),
    content = List(
      ConceptContent("<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>",
                     "nb"))
  )

  val concept7: Concept = TestData.sampleConcept.copy(
    id = Option(7),
    title = List(ConceptTitle("Yggdrasil livets tre", "nb")),
    content = List(ConceptContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb"))
  )

  val concept8: Concept = TestData.sampleConcept.copy(
    id = Option(8),
    title = List(ConceptTitle("Baldur har mareritt", "nb")),
    content = List(ConceptContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")),
    subjectIds = Set("urn:subject:10")
  )

  val concept9: Concept = TestData.sampleConcept.copy(
    id = Option(9),
    title = List(ConceptTitle("baldur har mareritt om Ragnarok", "nb")),
    content = List(ConceptContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")),
    tags = Seq(ConceptTags(Seq("stor", "klovn"), "nb")),
    subjectIds = Set("urn:subject:1", "urn:subject:100"),
    metaImage = Seq(ConceptMetaImage("test.image", "imagealt", "nb"), ConceptMetaImage("test.url2", "imagealt", "en"))
  )

  val concept10: Concept = TestData.sampleConcept.copy(
    id = Option(10),
    title = List(ConceptTitle("Unrelated", "en"), ConceptTitle("Urelatert", "nb")),
    content = List(ConceptContent("Pompel", "en"), ConceptContent("Pilt", "nb")),
    tags = Seq(ConceptTags(Seq("cageowl"), "en"), ConceptTags(Seq("burugle"), "nb")),
    updated = DateTime.now().minusDays(1).toDate,
    subjectIds = Set("urn:subject:2"),
    visualElement = List(
      VisualElement(
        """<embed data-resource="image" data-url="test.url" /><embed data-resource="video" data-url="test.url2" data-resource_id="test.id2" />""",
        "nb"))
  )

  val concept11: Concept = TestData.sampleConcept.copy(id = Option(11),
                                                       title = List(ConceptTitle("englando", "en")),
                                                       content = List(ConceptContent("englandocontent", "en")))

  val searchSettings = SearchSettings(
    withIdIn = List.empty,
    searchLanguage = DefaultLanguage,
    page = 1,
    pageSize = 10,
    sort = Sort.ByIdAsc,
    fallback = false,
    subjects = Set.empty,
    tagsToFilterBy = Set.empty,
    exactTitleMatch = false,
    shouldScroll = false,
    embedResource = None,
    embedId = None
  )

  override def beforeAll(): Unit = if (elasticSearchContainer.isSuccess) {
    publishedConceptIndexService.createIndexWithName(ConceptApiProperties.DraftConceptSearchIndex)

    publishedConceptIndexService.indexDocument(concept1)
    publishedConceptIndexService.indexDocument(concept2)
    publishedConceptIndexService.indexDocument(concept3)
    publishedConceptIndexService.indexDocument(concept4)
    publishedConceptIndexService.indexDocument(concept5)
    publishedConceptIndexService.indexDocument(concept6)
    publishedConceptIndexService.indexDocument(concept7)
    publishedConceptIndexService.indexDocument(concept8)
    publishedConceptIndexService.indexDocument(concept9)
    publishedConceptIndexService.indexDocument(concept10)
    publishedConceptIndexService.indexDocument(concept11)

    blockUntil(() => {
      publishedConceptSearchService.countDocuments == 11
    })
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    publishedConceptSearchService.getStartAtAndNumResults(0, 20001) should equal((0, ConceptApiProperties.MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    publishedConceptSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal(
      (expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * DefaultPageSize
    publishedConceptSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal(
      (expectedStartAt, DefaultPageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(results) =
      publishedConceptSearchService.all(searchSettings.copy(sort = Sort.ByIdAsc))
    val hits = results.results
    results.totalCount should be(10)
    hits.head.id should be(1)
    hits(1).id should be(2)
    hits(2).id should be(3)
    hits(3).id should be(4)
    hits(4).id should be(5)
    hits(5).id should be(6)
    hits(6).id should be(7)
    hits(7).id should be(8)
    hits(8).id should be(9)
    hits.last.id should be(10)
  }

  test("That all returns all documents ordered by id descending") {
    val Success(results) =
      publishedConceptSearchService.all(searchSettings.copy(sort = Sort.ByIdDesc))
    val hits = results.results
    results.totalCount should be(10)
    hits.head.id should be(10)
    hits.last.id should be(1)
  }

  test("That all returns all documents ordered by title ascending") {
    val Success(results) =
      publishedConceptSearchService.all(searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits = results.results

    results.totalCount should be(10)
    hits.head.id should be(8)
    hits(1).id should be(9)
    hits(2).id should be(1)
    hits(3).id should be(3)
    hits(4).id should be(5)
    hits(5).id should be(6)
    hits(6).id should be(2)
    hits(7).id should be(4)
    hits.last.id should be(7)
  }

  test("That all returns all documents ordered by title descending") {
    val Success(results) =
      publishedConceptSearchService.all(searchSettings.copy(sort = Sort.ByTitleDesc))
    val hits = results.results
    results.totalCount should be(10)
    hits.head.id should be(7)
    hits(1).id should be(10)
    hits(2).id should be(4)
    hits(3).id should be(2)
    hits(4).id should be(6)
    hits(5).id should be(5)
    hits(6).id should be(3)
    hits(7).id should be(1)
    hits(8).id should be(9)
    hits.last.id should be(8)

  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val Success(results) =
      publishedConceptSearchService.all(searchSettings.copy(sort = Sort.ByLastUpdatedDesc))
    val hits = results.results
    results.totalCount should be(10)
    hits.head.id should be(10)
    hits(1).id should be(5)
    hits(2).id should be(8)
    hits(3).id should be(9)
    hits(4).id should be(2)
    hits(5).id should be(4)
    hits(6).id should be(6)
    hits(7).id should be(1)
    hits(8).id should be(7)
    hits.last.id should be(3)

  }

  test("That all filtered by id only returns documents with the given ids") {
    val Success(results) =
      publishedConceptSearchService.all(searchSettings.copy(withIdIn = List(1, 3)))
    val hits = results.results
    results.totalCount should be(2)
    hits.head.id should be(1)
    hits.last.id should be(3)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(page1) =
      publishedConceptSearchService.all(searchSettings.copy(page = 1, pageSize = 2, sort = Sort.ByTitleAsc))
    val Success(page2) =
      publishedConceptSearchService.all(searchSettings.copy(page = 2, pageSize = 2, sort = Sort.ByTitleAsc))

    val hits1 = page1.results
    page1.totalCount should be(10)
    page1.page.get should be(1)
    hits1.size should be(2)
    hits1.head.id should be(8)
    hits1.last.id should be(9)

    val hits2 = page2.results
    page2.totalCount should be(10)
    page2.page.get should be(2)
    hits2.size should be(2)
    hits2.head.id should be(1)
    hits2.last.id should be(3)
  }

  test("That search matches title and content ordered by relevance descending") {
    val Success(results) =
      publishedConceptSearchService.matchingQuery("bil", searchSettings.copy(sort = Sort.ByRelevanceDesc))
    val hits = results.results

    results.totalCount should be(3)
    hits.head.id should be(5)
    hits(1).id should be(1)
    hits.last.id should be(3)
  }

  test("That search matches title") {
    val Success(results) =
      publishedConceptSearchService.matchingQuery("Pingvinen", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(2)
  }

  test("That search for title matches correct number of concepts") {
    val Success(results) =
      publishedConceptSearchService.matchingQuery("baldur har mareritt", searchSettings.copy(sort = Sort.ByTitleAsc))
    results.totalCount should be(2)
  }

  test("That search for title with exact parameter matches correct number of concepts") {
    val Success(results) =
      publishedConceptSearchService.matchingQuery("baldur har mareritt",
                                                  searchSettings.copy(sort = Sort.ByTitleAsc, exactTitleMatch = true))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(8)

    val Success(results2) =
      publishedConceptSearchService.matchingQuery("Pingvinen",
                                                  searchSettings.copy(sort = Sort.ByTitleAsc, exactTitleMatch = true))
    results2.totalCount should be(0)

    val Success(results3) =
      publishedConceptSearchService.matchingQuery("baldur har MARERITT",
                                                  searchSettings.copy(sort = Sort.ByTitleAsc, exactTitleMatch = true))
    val hits3 = results3.results
    results3.totalCount should be(1)
    hits3.head.id should be(8)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) =
      publishedConceptSearchService.matchingQuery("bilde + bil", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits1 = search1.results
    hits1.map(_.id) should equal(Seq(1, 3, 5))

    val Success(search2) =
      publishedConceptSearchService.matchingQuery("batmen + bil", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits2 = search2.results
    hits2.map(_.id) should equal(Seq(1))

    val Success(search3) =
      publishedConceptSearchService.matchingQuery("bil + bilde + -flaggermusmann",
                                                  searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits3 = search3.results
    hits3.map(_.id) should equal(Seq(3, 5))

    val Success(search4) =
      publishedConceptSearchService.matchingQuery("bil + -hulken", searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits4 = search4.results
    hits4.map(_.id) should equal(Seq(1, 3))
  }

  test("search in content should be ranked lower than title") {
    val Success(search) =
      publishedConceptSearchService.matchingQuery("mareritt + ragnarok",
                                                  searchSettings.copy(sort = Sort.ByRelevanceDesc))
    val hits = search.results
    hits.map(_.id) should equal(Seq(9, 8))
  }

  test("Search should return language it is matched in") {
    val Success(searchEn) =
      publishedConceptSearchService.matchingQuery("Unrelated", searchSettings.copy(searchLanguage = "*"))
    val Success(searchNb) =
      publishedConceptSearchService.matchingQuery("Urelatert", searchSettings.copy(searchLanguage = "*"))

    searchEn.totalCount should be(1)
    searchEn.results.head.title.language should be("en")
    searchEn.results.head.title.title should be("Unrelated")
    searchEn.results.head.content.language should be("en")
    searchEn.results.head.content.content should be("Pompel")

    searchNb.totalCount should be(1)
    searchNb.results.head.title.language should be("nb")
    searchNb.results.head.title.title should be("Urelatert")
    searchNb.results.head.content.language should be("nb")
    searchNb.results.head.content.content should be("Pilt")
  }

  test("Search for all languages should return all concepts in correct language") {
    val Success(search) =
      publishedConceptSearchService.all(searchSettings.copy(searchLanguage = Language.AllLanguages, pageSize = 100))
    val hits = search.results

    search.totalCount should equal(11)
    hits.head.id should be(1)
    hits.head.title.language should be("nb")
    hits(1).id should be(2)
    hits(2).id should be(3)
    hits(3).id should be(4)
    hits(4).id should be(5)
    hits(5).id should be(6)
    hits(6).id should be(7)
    hits(7).id should be(8)
    hits(8).id should be(9)
    hits(9).id should be(10)
    hits(9).title.language should be("nb")
    hits(10).id should be(11)
    hits(10).title.language should be("en")
  }

  test("That searching with fallback parameter returns concept in language priority even if doesnt match on language") {
    val Success(search) =
      publishedConceptSearchService.all(
        searchSettings.copy(withIdIn = List(9, 10, 11), searchLanguage = "en", fallback = true))

    search.totalCount should equal(3)
    search.results.head.id should equal(9)
    search.results.head.title.language should equal("nb")
    search.results(1).id should equal(10)
    search.results(1).title.language should equal("en")
    search.results(2).id should equal(11)
    search.results(2).title.language should equal("en")
  }

  test("That scrolling works as expected") {
    val pageSize = 2
    val expectedIds = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).sliding(pageSize, pageSize).toList

    val Success(initialSearch) =
      publishedConceptSearchService.all(
        searchSettings.copy(
          pageSize = pageSize,
          searchLanguage = "*",
          fallback = true,
          shouldScroll = true
        ))

    val Success(scroll1) = publishedConceptSearchService.scroll(initialSearch.scrollId.get, "*")
    val Success(scroll2) = publishedConceptSearchService.scroll(scroll1.scrollId.get, "*")
    val Success(scroll3) = publishedConceptSearchService.scroll(scroll2.scrollId.get, "*")
    val Success(scroll4) = publishedConceptSearchService.scroll(scroll3.scrollId.get, "*")
    val Success(scroll5) = publishedConceptSearchService.scroll(scroll4.scrollId.get, "*")
    val Success(scroll6) = publishedConceptSearchService.scroll(scroll5.scrollId.get, "*")

    initialSearch.results.map(_.id) should be(expectedIds.head)
    scroll1.results.map(_.id) should be(expectedIds(1))
    scroll2.results.map(_.id) should be(expectedIds(2))
    scroll3.results.map(_.id) should be(expectedIds(3))
    scroll4.results.map(_.id) should be(expectedIds(4))
    scroll5.results.map(_.id) should be(expectedIds(5))
    scroll6.results.map(_.id) should be(List.empty)
  }

  test("that filtering with subject id should work as expected") {
    val Success(search) =
      publishedConceptSearchService.all(searchSettings.copy(subjects = Set("urn:subject:1", "urn:subject:2")))

    search.totalCount should be(2)
    search.results.map(_.id) should be(Seq(9, 10))
  }

  test("that filtering for tags works as expected and respects language/fallback") {
    val Success(search) = publishedConceptSearchService.all(searchSettings.copy(tagsToFilterBy = Set("burugle")))
    search.totalCount should be(1)
    search.results.map(_.id) should be(Seq(10))

    val Success(search1) =
      publishedConceptSearchService.all(searchSettings.copy(tagsToFilterBy = Set("burugle"), searchLanguage = "*"))
    search1.totalCount should be(1)
    search1.results.map(_.id) should be(Seq(10))

    val Success(search2) =
      publishedConceptSearchService.all(searchSettings.copy(tagsToFilterBy = Set("burugle"), searchLanguage = "en"))
    search2.totalCount should be(0)

    val Success(search3) =
      publishedConceptSearchService.all(
        searchSettings.copy(tagsToFilterBy = Set("burugle"), searchLanguage = "en", fallback = true))
    search3.totalCount should be(1)
    search3.results.map(_.id) should be(Seq(10))
  }

  test("That tag search works as expected") {
    val Success(tagSearch1) =
      publishedConceptSearchService.getTagsWithSubjects(List("urn:subject:2", "urn:subject:100"), "nb", false)
    val Success(tagSearch2) =
      publishedConceptSearchService.getTagsWithSubjects(List("urn:subject:2", "urn:subject:100"), "en", false)

    tagSearch1 should be(
      List(
        SubjectTags(
          subjectId = "urn:subject:2",
          tags = List("burugle"),
          language = "nb"
        ),
        SubjectTags(
          subjectId = "urn:subject:100",
          tags = List("stor", "klovn"),
          language = "nb"
        )
      ))

    tagSearch2 should be(
      List(
        SubjectTags(
          subjectId = "urn:subject:2",
          tags = List("cageowl"),
          language = "en"
        )
      ))
  }

  test("That tag search works as expected with fallback") {
    val Success(tagSearch1) =
      publishedConceptSearchService.getTagsWithSubjects(List("urn:subject:2", "urn:subject:100"), "nb", true)
    val Success(tagSearch2) =
      publishedConceptSearchService.getTagsWithSubjects(List("urn:subject:2", "urn:subject:100"), "en", true)

    tagSearch1 should be(
      List(
        SubjectTags(
          subjectId = "urn:subject:2",
          tags = List("burugle"),
          language = "nb"
        ),
        SubjectTags(
          subjectId = "urn:subject:100",
          tags = List("stor", "klovn"),
          language = "nb"
        )
      ))

    tagSearch2 should be(
      List(
        SubjectTags(
          subjectId = "urn:subject:2",
          tags = List("cageowl"),
          language = "en"
        ),
        SubjectTags(
          subjectId = "urn:subject:100",
          tags = List("stor", "klovn"),
          language = "nb"
        ),
      ))
  }

  test("that search on embedId matches visual element") {
    val Success(search) =
      publishedConceptSearchService.all(
        searchSettings.copy(embedId = Some("test.url"), searchLanguage = Language.AllLanguages))

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("that search on embedResource matches visual element") {
    val Success(search) =
      publishedConceptSearchService.all(
        searchSettings.copy(embedResource = Some("video"), searchLanguage = Language.AllLanguages))

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("that search on embedId matches meta image") {
    val Success(search) =
      publishedConceptSearchService.all(
        searchSettings.copy(embedId = Some("test.image"), searchLanguage = Language.AllLanguages))

    search.totalCount should be(1)
    search.results.head.id should be(9)
  }

  test("that search on query parameter as embedId matches meta image") {
    val Success(search) =
      publishedConceptSearchService.matchingQuery(
        "test.image",
        searchSettings.copy()
      )

    search.totalCount should be(1)
    search.results.head.id should be(9)
  }

  test("that search on query parameter as embedResource matches visual element") {
    val Success(search) =
      publishedConceptSearchService.matchingQuery(
        "video",
        searchSettings.copy()
      )

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("that search on query parameter as embedId matches visual element") {
    val Success(search) =
      publishedConceptSearchService.matchingQuery(
        "test.url",
        searchSettings.copy()
      )

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("that search on query parameter matches on concept id") {
    val Success(search) =
      publishedConceptSearchService.matchingQuery(
        "2",
        searchSettings.copy()
      )

    search.totalCount should be(1)
    search.results.head.id should be(2)
  }

  test("that search on embedId and embedResource only returns results with an embed matching both params") {
    val Success(search) =
      publishedConceptSearchService.all(
        searchSettings
          .copy(embedId = Some("test.url2"), embedResource = Some("image"), searchLanguage = Language.AllLanguages))

    search.totalCount should be(1)
    search.results.head.id should be(9)
  }

  test("That search on embed id supports embed with multiple id attributes") {
    val Success(search1) =
      publishedConceptSearchService.all(
        searchSettings.copy(embedId = Some("test.url2"))
      )
    val Success(search2) =
      publishedConceptSearchService.all(
        searchSettings.copy(embedId = Some("test.id2"))
      )

    search1.totalCount should be(1)
    search1.results.head.id should be(10)
    search2.totalCount should be(1)
    search2.results.head.id should be(10)

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
