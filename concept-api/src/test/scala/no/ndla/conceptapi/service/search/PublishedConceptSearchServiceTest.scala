/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.service.search

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.domain.draft.DraftCopyright
import no.ndla.common.model.domain.{Author, ContributorType, Tag, Title}
import no.ndla.conceptapi.model.domain.*
import no.ndla.conceptapi.model.search
import no.ndla.conceptapi.*
import no.ndla.language.Language
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite

import java.time.LocalDateTime
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.concept.{Concept, ConceptContent, ConceptType, GlossData, VisualElement, WordClass}
import no.ndla.conceptapi.service.ConverterService
import no.ndla.mapping.License
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}
import no.ndla.search.model.domain.{Bucket, TermAggregation}

class PublishedConceptSearchServiceTest extends ElasticsearchIntegrationSuite with TestEnvironment {
  override implicit lazy val searchLanguage: SearchLanguage = new SearchLanguage
  override implicit lazy val e4sClient: NdlaE4sClient       =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))
  override implicit lazy val publishedConceptSearchService: PublishedConceptSearchService =
    new PublishedConceptSearchService
  override implicit lazy val publishedConceptIndexService: PublishedConceptIndexService =
    new PublishedConceptIndexService {
      override val indexShards = 1
    }
  override implicit lazy val converterService: ConverterService             = new ConverterService
  override implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService

  val byNcSa: DraftCopyright = DraftCopyright(
    Some(License.CC_BY_NC_SA.toString),
    Some("Gotham City"),
    List(Author(ContributorType.Writer, "DC Comics")),
    List(),
    List(),
    None,
    None,
    false,
  )

  val publicDomain: DraftCopyright = DraftCopyright(
    Some(License.PublicDomain.toString),
    Some("Metropolis"),
    List(Author(ContributorType.Writer, "Bruce Wayne")),
    List(),
    List(),
    None,
    None,
    false,
  )

  val copyrighted: DraftCopyright = DraftCopyright(
    Some(License.Copyrighted.toString),
    Some("New York"),
    List(Author(ContributorType.Writer, "Clark Kent")),
    List(),
    List(),
    None,
    None,
    false,
  )

  val today: LocalDateTime = LocalDateTime.now()

  val concept1: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(1),
      title = List(Title("Batmen er på vift med en bil", "nb")),
      content = List(
        ConceptContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")
      ),
      copyright = Some(publicDomain),
    )

  val concept2: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(2),
      title = List(Title("pingvinen er ute og går", "nb")),
      content = List(ConceptContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb")),
      copyright = Some(publicDomain),
    )

  val concept3: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(3),
      title = List(Title("Donald Duck kjører bil", "nb")),
      content = List(ConceptContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb")),
      copyright = Some(copyrighted),
    )

  val concept4: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(4),
      title = List(Title("Superman er ute og flyr", "nb")),
      content =
        List(ConceptContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb")),
      copyright = Some(copyrighted),
    )

  val concept5: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(5),
      title = List(Title("Hulken løfter biler", "nb")),
      content = List(ConceptContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb")),
      copyright = Some(byNcSa),
    )

  val concept6: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(6),
      title = List(Title("Loke og Tor prøver å fange midgaardsormen", "nb")),
      content = List(
        ConceptContent(
          "<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>",
          "nb",
        )
      ),
      copyright = Some(byNcSa),
    )

  val concept7: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(7),
      title = List(Title("Yggdrasil livets tre", "nb")),
      content = List(ConceptContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb")),
      copyright = Some(byNcSa),
    )

  val concept8: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(8),
      title = List(Title("Baldur har mareritt", "nb")),
      content = List(ConceptContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")),
      copyright = Some(byNcSa),
    )

  val concept9: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(9),
      title = List(Title("baldur har mareritt om Ragnarok", "nb")),
      content = List(ConceptContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")),
      copyright = Some(byNcSa),
      tags = Seq(Tag(Seq("stor", "klovn"), "nb")),
    )

  val concept10: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(10),
      title = List(Title("Unrelated", "en"), Title("Urelatert", "nb")),
      content = List(ConceptContent("Pompel", "en"), ConceptContent("Pilt", "nb")),
      copyright = Some(byNcSa),
      updated = NDLADate.now().minusDays(1),
      tags = Seq(Tag(Seq("cageowl"), "en"), Tag(Seq("burugle"), "nb")),
      visualElement = List(
        VisualElement(
          s"""<$EmbedTagName data-resource="image" data-url="test.url"></$EmbedTagName><$EmbedTagName data-resource="brightcove" data-url="test.url2" data-videoid="test.id2"></$EmbedTagName>""",
          "nb",
        )
      ),
    )

  val concept11: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(11),
      title = List(Title("\"englando\"", "en")),
      content = List(ConceptContent("englandocontent", "en")),
      copyright = Some(byNcSa),
    )

  val concept12: Concept = TestData
    .sampleConcept
    .copy(
      id = Option(12),
      title = List(Title("glose", "nb")),
      content = List(ConceptContent("glose", "nb")),
      copyright = Some(byNcSa),
      conceptType = ConceptType.GLOSS,
      glossData = Some(
        GlossData(
          gloss = "glossorama",
          wordClass = WordClass.NOUN,
          originalLanguage = "de",
          transcriptions = Map.empty,
          examples = List.empty,
        )
      ),
    )

  val searchSettings: search.SearchSettings = search.SearchSettings(
    withIdIn = List.empty,
    searchLanguage = props.DefaultLanguage,
    page = 1,
    pageSize = 10,
    sort = Sort.ByIdAsc,
    fallback = false,
    tagsToFilterBy = Set.empty,
    exactTitleMatch = false,
    shouldScroll = false,
    embedResource = List.empty,
    embedId = None,
    conceptType = None,
    aggregatePaths = List.empty,
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (elasticSearchContainer.isSuccess) {
      publishedConceptIndexService.createIndexWithName(props.DraftConceptSearchIndex)

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
      publishedConceptIndexService.indexDocument(concept12)

      blockUntil(() => {
        publishedConceptSearchService.countDocuments == 12
      })
    }
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    publishedConceptSearchService.getStartAtAndNumResults(0, 20001) should equal((0, props.MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size"
  ) {
    val page            = 74
    val expectedStartAt = (page - 1) * props.DefaultPageSize
    publishedConceptSearchService.getStartAtAndNumResults(page, props.DefaultPageSize) should equal(
      (expectedStartAt, props.DefaultPageSize)
    )
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page            = 123
    val expectedStartAt = (page - 1) * props.DefaultPageSize
    publishedConceptSearchService.getStartAtAndNumResults(page, props.DefaultPageSize) should equal(
      (expectedStartAt, props.DefaultPageSize)
    )
  }

  test("That all returns all documents ordered by id ascending") {
    val results = publishedConceptSearchService.all(searchSettings.copy(pageSize = 20, sort = Sort.ByIdAsc)).get
    val hits    = results.results
    results.totalCount should be(11)
    hits.head.id should be(1)
    hits(1).id should be(2)
    hits(2).id should be(3)
    hits(3).id should be(4)
    hits(4).id should be(5)
    hits(5).id should be(6)
    hits(6).id should be(7)
    hits(7).id should be(8)
    hits(8).id should be(9)
    hits(9).id should be(10)
    hits.last.id should be(12)
  }

  test("That all returns all documents ordered by id descending") {
    val results = publishedConceptSearchService.all(searchSettings.copy(pageSize = 20, sort = Sort.ByIdDesc)).get
    val hits    = results.results
    results.totalCount should be(11)
    hits.head.id should be(12)
    hits.last.id should be(1)
  }

  test("That all returns all documents ordered by title ascending") {
    val results = publishedConceptSearchService.all(searchSettings.copy(pageSize = 20, sort = Sort.ByTitleAsc)).get
    val hits    = results.results

    results.totalCount should be(11)
    hits.head.id should be(8)
    hits(1).id should be(9)
    hits(2).id should be(1)
    hits(3).id should be(3)
    hits(4).id should be(12)
    hits(5).id should be(5)
    hits(6).id should be(6)
    hits(7).id should be(2)
    hits(8).id should be(4)
    hits.last.id should be(7)
  }

  test("That all returns all documents ordered by title descending") {
    val results = publishedConceptSearchService.all(searchSettings.copy(pageSize = 20, sort = Sort.ByTitleDesc)).get
    val hits    = results.results
    results.totalCount should be(11)
    hits.head.id should be(7)
    hits(1).id should be(10)
    hits(2).id should be(4)
    hits(3).id should be(2)
    hits(4).id should be(6)
    hits(5).id should be(5)
    hits(6).id should be(12)
    hits(7).id should be(3)
    hits(8).id should be(1)
    hits(9).id should be(9)
    hits.last.id should be(8)

  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val results = publishedConceptSearchService
      .all(searchSettings.copy(pageSize = 20, sort = Sort.ByLastUpdatedDesc))
      .get
    val hits = results.results
    results.totalCount should be(11)
    hits.map(_.id) should be(Seq(10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12))
  }

  test("That all filtered by id only returns documents with the given ids") {
    val results = publishedConceptSearchService.all(searchSettings.copy(withIdIn = List(1, 3))).get
    val hits    = results.results
    results.totalCount should be(2)
    hits.head.id should be(1)
    hits.last.id should be(3)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = publishedConceptSearchService
      .all(searchSettings.copy(page = 1, pageSize = 2, sort = Sort.ByTitleAsc))
      .get
    val page2 = publishedConceptSearchService
      .all(searchSettings.copy(page = 2, pageSize = 2, sort = Sort.ByTitleAsc))
      .get

    val hits1 = page1.results
    page1.totalCount should be(11)
    page1.page.get should be(1)
    hits1.size should be(2)
    hits1.head.id should be(8)
    hits1.last.id should be(9)

    val hits2 = page2.results
    page2.totalCount should be(11)
    page2.page.get should be(2)
    hits2.size should be(2)
    hits2.head.id should be(1)
    hits2.last.id should be(3)
  }

  test("That search matches title and content ordered by relevance descending") {
    val results = publishedConceptSearchService
      .matchingQuery("bil", searchSettings.copy(sort = Sort.ByRelevanceDesc))
      .get
    val hits = results.results

    results.totalCount should be(3)
    hits.map(_.id) should be(Seq(1, 5, 3))
  }

  test("That search matches title") {
    val results = publishedConceptSearchService
      .matchingQuery("Pingvinen", searchSettings.copy(sort = Sort.ByTitleAsc))
      .get
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(2)
  }

  test("That search for title matches correct number of concepts") {
    val results = publishedConceptSearchService
      .matchingQuery("baldur har mareritt", searchSettings.copy(sort = Sort.ByTitleAsc))
      .get
    results.totalCount should be(2)
  }

  test("That search for title with exact parameter matches correct number of concepts") {
    val results = publishedConceptSearchService
      .matchingQuery("baldur har mareritt", searchSettings.copy(sort = Sort.ByTitleAsc, exactTitleMatch = true))
      .get
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(8)

    val results2 = publishedConceptSearchService
      .matchingQuery("Pingvinen", searchSettings.copy(sort = Sort.ByTitleAsc, exactTitleMatch = true))
      .get
    results2.totalCount should be(0)

    val results3 = publishedConceptSearchService
      .matchingQuery("baldur har MARERITT", searchSettings.copy(sort = Sort.ByTitleAsc, exactTitleMatch = true))
      .get
    val hits3 = results3.results
    results3.totalCount should be(1)
    hits3.head.id should be(8)
  }

  test("Searching with logical AND only returns results with all terms") {
    val search1 = publishedConceptSearchService
      .matchingQuery("bilde + bil", searchSettings.copy(sort = Sort.ByTitleAsc))
      .get
    val hits1 = search1.results
    hits1.map(_.id) should equal(Seq(1, 3, 5))

    val search2 = publishedConceptSearchService
      .matchingQuery("batmen + bil", searchSettings.copy(sort = Sort.ByTitleAsc))
      .get
    val hits2 = search2.results
    hits2.map(_.id) should equal(Seq(1))

    val search3 = publishedConceptSearchService
      .matchingQuery("bil + bilde + -flaggermusmann", searchSettings.copy(sort = Sort.ByTitleAsc))
      .get
    val hits3 = search3.results
    hits3.map(_.id) should equal(Seq(3, 5))

    val search4 = publishedConceptSearchService
      .matchingQuery("bil + -hulken", searchSettings.copy(sort = Sort.ByTitleAsc))
      .get
    val hits4 = search4.results
    hits4.map(_.id) should equal(Seq(1, 3))
  }

  test("search in content should be ranked lower than title") {
    val search = publishedConceptSearchService
      .matchingQuery("mareritt + ragnarok", searchSettings.copy(sort = Sort.ByRelevanceDesc))
      .get
    val hits = search.results
    hits.map(_.id) should equal(Seq(9, 8))
  }

  test("Search should return language it is matched in") {
    val searchEn = publishedConceptSearchService
      .matchingQuery("Unrelated", searchSettings.copy(searchLanguage = "*"))
      .get
    val searchNb = publishedConceptSearchService
      .matchingQuery("Urelatert", searchSettings.copy(searchLanguage = "*"))
      .get

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
    val search = publishedConceptSearchService
      .all(searchSettings.copy(searchLanguage = Language.AllLanguages, pageSize = 100))
      .get
    val hits = search.results

    search.totalCount should equal(12)
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
    val search = publishedConceptSearchService
      .all(searchSettings.copy(withIdIn = List(9, 10, 11), searchLanguage = "en", fallback = true))
      .get

    search.totalCount should equal(3)
    search.results.head.id should equal(9)
    search.results.head.title.language should equal("nb")
    search.results(1).id should equal(10)
    search.results(1).title.language should equal("en")
    search.results(2).id should equal(11)
    search.results(2).title.language should equal("en")
  }

  test("That scrolling works as expected") {
    val pageSize    = 2
    val expectedIds = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12).sliding(pageSize, pageSize).toList

    val initialSearch = publishedConceptSearchService
      .all(searchSettings.copy(searchLanguage = "*", pageSize = pageSize, fallback = true, shouldScroll = true))
      .get

    val scroll1 = publishedConceptSearchService.scroll(initialSearch.scrollId.get, "*").get
    val scroll2 = publishedConceptSearchService.scroll(scroll1.scrollId.get, "*").get
    val scroll3 = publishedConceptSearchService.scroll(scroll2.scrollId.get, "*").get
    val scroll4 = publishedConceptSearchService.scroll(scroll3.scrollId.get, "*").get
    val scroll5 = publishedConceptSearchService.scroll(scroll4.scrollId.get, "*").get
    val scroll6 = publishedConceptSearchService.scroll(scroll5.scrollId.get, "*").get

    initialSearch.results.map(_.id) should be(expectedIds.head)
    scroll1.results.map(_.id) should be(expectedIds(1))
    scroll2.results.map(_.id) should be(expectedIds(2))
    scroll3.results.map(_.id) should be(expectedIds(3))
    scroll4.results.map(_.id) should be(expectedIds(4))
    scroll5.results.map(_.id) should be(expectedIds(5))
    scroll6.results.map(_.id) should be(List.empty)
  }

  test("that filtering for tags works as expected and respects language/fallback") {
    val search = publishedConceptSearchService.all(searchSettings.copy(tagsToFilterBy = Set("burugle"))).get
    search.totalCount should be(1)
    search.results.map(_.id) should be(Seq(10))

    val search1 = publishedConceptSearchService
      .all(searchSettings.copy(searchLanguage = "*", tagsToFilterBy = Set("burugle")))
      .get
    search1.totalCount should be(1)
    search1.results.map(_.id) should be(Seq(10))

    val search2 = publishedConceptSearchService
      .all(searchSettings.copy(searchLanguage = "en", tagsToFilterBy = Set("burugle")))
      .get
    search2.totalCount should be(0)

    val search3 = publishedConceptSearchService
      .all(searchSettings.copy(searchLanguage = "en", fallback = true, tagsToFilterBy = Set("burugle")))
      .get
    search3.totalCount should be(1)
    search3.results.map(_.id) should be(Seq(10))
  }

  test("that search on embedId matches visual element") {
    val search = publishedConceptSearchService
      .all(searchSettings.copy(searchLanguage = Language.AllLanguages, embedId = Some("test.url")))
      .get

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("that search on embedResource matches visual element") {
    val search = publishedConceptSearchService
      .all(searchSettings.copy(searchLanguage = Language.AllLanguages, embedResource = List("brightcove")))
      .get

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("that search on embedId matches meta image") {
    val search = publishedConceptSearchService
      .all(searchSettings.copy(searchLanguage = Language.AllLanguages, embedId = Some("test.url")))
      .get

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("that search on query parameter as embedId matches meta image") {
    val search = publishedConceptSearchService.matchingQuery("test.url", searchSettings.copy()).get

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("that search on query parameter as embedResource matches visual element") {
    val search = publishedConceptSearchService.matchingQuery("brightcove", searchSettings.copy()).get

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("that search on query parameter as embedId matches visual element") {
    val search = publishedConceptSearchService.matchingQuery("test.url", searchSettings.copy()).get

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("that search on query parameter matches on concept id") {
    val search = publishedConceptSearchService.matchingQuery("2", searchSettings.copy()).get

    search.totalCount should be(1)
    search.results.head.id should be(2)
  }

  test("that search on embedId and embedResource only returns results with an embed matching both params") {
    val search = publishedConceptSearchService
      .all(
        searchSettings.copy(
          searchLanguage = Language.AllLanguages,
          embedResource = List("image"),
          embedId = Some("test.url"),
        )
      )
      .get

    search.totalCount should be(1)
    search.results.head.id should be(10)
  }

  test("That search on embed id supports embed with multiple id attributes") {
    val search1 = publishedConceptSearchService.all(searchSettings.copy(embedId = Some("test.url2"))).get
    val search2 = publishedConceptSearchService.all(searchSettings.copy(embedId = Some("test.id2"))).get

    search1.totalCount should be(1)
    search1.results.head.id should be(10)
    search2.totalCount should be(1)
    search2.results.head.id should be(10)

  }

  test("That search on exactTitleMatch only matches exact") {
    val search1 = publishedConceptSearchService
      .matchingQuery("\"urelatert noe noe\"", searchSettings.copy(fallback = true, exactTitleMatch = true))
      .get
    search1.totalCount should be(0)

    val search2 = publishedConceptSearchService
      .matchingQuery("et urelatert noe noe", searchSettings.copy(fallback = true, exactTitleMatch = true))
      .get
    search2.totalCount should be(0)

    val search3 = publishedConceptSearchService
      .matchingQuery("\"englando\"", searchSettings.copy(fallback = true, exactTitleMatch = true))
      .get
    search3.totalCount should be(1)
    search3.results.head.id should be(11)

    val search4 = publishedConceptSearchService
      .matchingQuery("unrelated", searchSettings.copy(fallback = true, exactTitleMatch = true))
      .get
    search4.totalCount should be(1)
    search4.results.head.id should be(10)

    val search5 = publishedConceptSearchService
      .matchingQuery("batmen", searchSettings.copy(fallback = true, exactTitleMatch = true))
      .get
    search5.totalCount should be(0)

  }

  test("search results should return copyright info") {
    val search = publishedConceptSearchService
      .matchingQuery("hulk", searchSettings.copy(sort = Sort.ByRelevanceDesc))
      .get
    val hits = search.results
    hits.map(_.id) should equal(Seq(5))
    hits.head.copyright.head.origin should be(Some("Gotham City"))
    hits.head.copyright.head.creators.length should be(1)
  }

  test("filtering on conceptType should work as expected") {
    {
      val search = publishedConceptSearchService.all(searchSettings.copy(conceptType = Some("concept"))).get
      search.totalCount should be(10)
    }
    {
      val search = publishedConceptSearchService.all(searchSettings.copy(conceptType = Some("gloss"))).get
      search.totalCount should be(1)
    }
  }

  test("That searching for gloss data matches") {
    val search = publishedConceptSearchService.matchingQuery("glossorama", searchSettings).get
    search.totalCount should be(1)
    search.results.head.id should be(12)
  }

  test("Aggregating works as expected") {
    val settings = searchSettings.copy(aggregatePaths = List("license"))

    val expectedAggregations = TermAggregation(
      field = Seq("license"),
      sumOtherDocCount = 0,
      docCountErrorUpperBound = 0,
      buckets = Seq(
        Bucket(License.CC_BY_NC_SA.toString, 7),
        Bucket(License.Copyrighted.toString, 2),
        Bucket(License.PublicDomain.toString, 2),
      ),
    )

    val result = publishedConceptSearchService.all(settings).get
    result.aggregations should be(Seq(expectedAggregations))
  }

}
