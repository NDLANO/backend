/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import no.ndla.articleapi.*
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.domain.*
import no.ndla.articleapi.service.ConverterService
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.*
import no.ndla.common.model.domain.article.Copyright
import no.ndla.language.Language
import no.ndla.mapping.License.{CC_BY_NC_SA, Copyrighted, PublicDomain}
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

class ArticleSearchServiceTest extends ElasticsearchIntegrationSuite with UnitSuite with TestEnvironment {
  override given e4sClient: NdlaE4sClient =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override implicit lazy val searchLanguage: SearchLanguage             = new SearchLanguage
  override implicit lazy val articleSearchService: ArticleSearchService = new ArticleSearchService
  override implicit lazy val articleIndexService: ArticleIndexService   = new ArticleIndexService {
    override val indexShards = 1
  }
  override implicit lazy val converterService: ConverterService             = new ConverterService
  override implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService

  val byNcSa: Copyright =
    Copyright(
      CC_BY_NC_SA.toString,
      Some("Gotham City"),
      List(Author(ContributorType.Writer, "DC Comics")),
      List(),
      List(),
      None,
      None,
      false
    )

  val publicDomain: Copyright =
    Copyright(
      PublicDomain.toString,
      Some("Metropolis"),
      List(Author(ContributorType.Writer, "Bruce Wayne")),
      List(),
      List(),
      None,
      None,
      false
    )

  val copyrighted: Copyright =
    Copyright(
      Copyrighted.toString,
      Some("New York"),
      List(Author(ContributorType.Writer, "Clark Kent")),
      List(),
      List(),
      None,
      None,
      false
    )

  val today: NDLADate = NDLADate.now()

  val article1: article.Article = TestData.sampleArticleWithByNcSa.copy(
    id = Option(1),
    title = List(Title("Batmen er på vift med en bil", "nb")),
    introduction = List(Introduction("Batmen", "nb")),
    content = List(
      ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")
    ),
    tags = List(Tag(List("fugl"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(3),
    metaImage = List(ArticleMetaImage("5555", "Alt text is here friend", "nb")),
    grepCodes = Seq("KV123", "KV456")
  )

  val article2: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(2),
    title = List(Title("Pingvinen er ute og går", "nb")),
    introduction = List(Introduction("Pingvinen", "nb")),
    content = List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb")),
    tags = List(Tag(List("fugl"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(2),
    grepCodes = Seq("KV123", "KV456")
  )

  val article3: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(3),
    title = List(Title("Donald Duck kjører bil", "nb")),
    introduction = List(Introduction("Donald Duck", "nb")),
    content = List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb")),
    tags = List(Tag(List("and"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(1),
    grepCodes = Seq("KV456")
  )

  val article4: article.Article = TestData.sampleArticleWithCopyrighted.copy(
    id = Option(4),
    title = List(Title("Superman er ute og flyr", "nb")),
    introduction = List(Introduction("Superman", "nb")),
    content =
      List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb")),
    tags = List(Tag(List("supermann"), "nb")),
    created = today.minusDays(4),
    updated = today
  )

  val article5: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(5),
    title = List(Title("Hulken løfter biler", "nb")),
    introduction = List(Introduction("Hulken", "nb")),
    content = List(ArticleContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb")),
    tags = List(Tag(List("hulk"), "nb")),
    created = today.minusDays(40),
    updated = today.minusDays(35)
  )

  val article6: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(6),
    title = List(Title("Loke og Tor prøver å fange midgaardsormen", "nb")),
    introduction = List(Introduction("Loke og Tor", "nb")),
    content = List(
      ArticleContent(
        "<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>",
        "nb"
      )
    ),
    tags = List(Tag(List("Loke", "Tor", "Naglfar"), "nb")),
    created = today.minusDays(30),
    updated = today.minusDays(25)
  )

  val article7: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(7),
    title = List(Title("Yggdrasil livets tre", "nb")),
    introduction = List(Introduction("Yggdrasil", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb")),
    tags = List(Tag(List("yggdrasil"), "nb")),
    created = today.minusDays(20),
    updated = today.minusDays(15)
  )

  val article8: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(8),
    title = List(Title("Baldur har mareritt", "nb")),
    introduction = List(Introduction("Baldur", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")),
    tags = List(Tag(List("baldur"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val article9: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(9),
    title = List(Title("En Baldur har mareritt om Ragnarok", "nb")),
    introduction = List(Introduction("Baldur", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")),
    tags = List(Tag(List("baldur"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val article10: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(10),
    title = List(Title("This article is in english", "en")),
    introduction = List(Introduction("Engulsk", "en")),
    content = List(ArticleContent("<p>Something something <em>english</em> What about", "en")),
    tags = List(Tag(List("englando"), "en")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val article11: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(11),
    title = List(Title("Katter", "nb"), Title("Cats", "en"), Title("Baloi", "biz")),
    introduction = List(
      Introduction("Katter er store", "nb"),
      Introduction("Cats are big", "en"),
      Introduction("Cats are baloi", "biz")
    ),
    metaDescription = List(Description("hurr durr ima sheep", "en")),
    content = List(ArticleContent("<p>Noe om en katt</p>", "nb"), ArticleContent("<p>Something about a cat</p>", "en")),
    tags = List(Tag(List("ikkehund"), "nb"), Tag(List("notdog"), "en")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val article12: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(12),
    title = List(Title("availability - Hemmelig lærer artikkel", "nb")),
    introduction = List(Introduction("Lærer", "nb")),
    metaDescription = List(Description("lærer", "nb")),
    content = List(ArticleContent("<p>Lærer</p>", "nb")),
    tags = List(Tag(List("lærer"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.Standard,
    availability = Availability.teacher
  )

  val article13: article.Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(13),
    title = List(Title("availability - Hemmelig student artikkel", "nb")),
    introduction = List(Introduction("Student", "nb")),
    metaDescription = List(Description("student", "nb")),
    content = List(ArticleContent("<p>Student</p>", "nb")),
    tags = List(Tag(List("student"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.Standard,
    availability = Availability.everyone
  )

  override def beforeAll(): Unit = if (elasticSearchContainer.isSuccess) {
    super.beforeAll()
    articleIndexService.createIndexAndAlias().get

    articleIndexService.indexDocument(article1).get
    articleIndexService.indexDocument(article2).get
    articleIndexService.indexDocument(article3).get
    articleIndexService.indexDocument(article4).get
    articleIndexService.indexDocument(article5).get
    articleIndexService.indexDocument(article6).get
    articleIndexService.indexDocument(article7).get
    articleIndexService.indexDocument(article8).get
    articleIndexService.indexDocument(article9).get
    articleIndexService.indexDocument(article10).get
    articleIndexService.indexDocument(article11).get
    articleIndexService.indexDocument(article12).get
    articleIndexService.indexDocument(article13).get

    blockUntil(() => articleSearchService.countDocuments == 13)
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    articleSearchService.getStartAtAndNumResults(0, 10001) should equal((0, props.MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size"
  ) {
    val page            = 74
    val expectedStartAt = (page - 1) * props.DefaultPageSize
    articleSearchService.getStartAtAndNumResults(page, props.DefaultPageSize) should equal(
      (expectedStartAt, props.DefaultPageSize)
    )
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page            = 123
    val expectedStartAt = (page - 1) * props.DefaultPageSize
    articleSearchService.getStartAtAndNumResults(page, props.DefaultPageSize) should equal(
      (expectedStartAt, props.DefaultPageSize)
    )
  }

  test("searching should return only articles of a given type if a type filter is specified") {
    val results = articleSearchService
      .matchingQuery(TestData.testSettings.copy(articleTypes = Seq(ArticleType.TopicArticle.entryName)))
      .get
    results.totalCount should be(3)

    val results2 = articleSearchService.matchingQuery(TestData.testSettings.copy(articleTypes = Seq.empty)).get
    results2.totalCount should be(10)
  }

  test("That searching without query returns all documents ordered by id ascending") {
    val results = articleSearchService.matchingQuery(TestData.testSettings.copy(query = None)).get
    val hits    = results.results
    results.totalCount should be(10)
    hits.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 8, 9, 11, 13))
  }

  test("That searching returns all documents ordered by id descending") {
    val results = articleSearchService.matchingQuery(TestData.testSettings.copy(sort = Sort.ByIdDesc)).get
    val hits    = results.results
    results.totalCount should be(10)
    hits.map(_.id) should be(Seq(13, 11, 9, 8, 7, 6, 5, 3, 2, 1))
  }

  test("That searching returns all documents ordered by title ascending") {
    val results = articleSearchService.matchingQuery(TestData.testSettings.copy(sort = Sort.ByTitleAsc)).get
    val hits    = results.results
    results.totalCount should be(10)
    hits.map(_.id) should be(Seq(8, 1, 3, 9, 5, 11, 6, 2, 7, 13))
  }

  test("That searching returns all documents ordered by title descending") {
    val results = articleSearchService.matchingQuery(TestData.testSettings.copy(sort = Sort.ByTitleDesc)).get
    val hits    = results.results
    results.totalCount should be(10)
    hits.map(_.id) should be(Seq(13, 7, 2, 6, 11, 5, 9, 3, 1, 8))
  }

  test("That searching returns all documents ordered by lastUpdated descending") {
    val results = articleSearchService.matchingQuery(TestData.testSettings.copy(sort = Sort.ByLastUpdatedDesc))
    val hits    = results.get.results
    results.get.totalCount should be(10)
    hits.map(_.id) should be(Seq(3, 2, 1, 8, 9, 11, 13, 7, 6, 5))
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val results = articleSearchService.matchingQuery(TestData.testSettings.copy(sort = Sort.ByLastUpdatedAsc)).get
    val hits    = results.results
    results.totalCount should be(10)
    hits.map(_.id) should be(Seq(5, 6, 7, 8, 9, 11, 13, 1, 2, 3))
  }

  test("That all filtering on license only returns documents with given license") {
    val results = articleSearchService
      .matchingQuery(TestData.testSettings.copy(sort = Sort.ByTitleAsc, license = Some(PublicDomain.toString)))
      .get
    val hits = results.results
    results.totalCount should be(9)
    hits.map(_.id) should be(Seq(8, 3, 9, 5, 11, 6, 2, 7, 13))
  }

  test("That all filtered by id only returns documents with the given ids") {
    val results = articleSearchService.matchingQuery(TestData.testSettings.copy(withIdIn = List(1, 3))).get
    val hits    = results.results
    results.totalCount should be(2)
    hits.head.id should be(1)
    hits.last.id should be(3)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 =
      articleSearchService.matchingQuery(TestData.testSettings.copy(sort = Sort.ByTitleAsc, page = 1, pageSize = 2)).get
    val page2 =
      articleSearchService.matchingQuery(TestData.testSettings.copy(sort = Sort.ByTitleAsc, page = 2, pageSize = 2)).get

    val hits1 = page1.results
    val hits2 = page2.results
    page1.totalCount should be(10)
    page1.page.get should be(1)
    hits1.size should be(2)
    hits1.head.id should be(8)
    hits1.last.id should be(1)
    page2.totalCount should be(10)
    page2.page.get should be(2)
    hits2.size should be(2)
    hits2.head.id should be(3)
    hits2.last.id should be(9)
  }

  test("matchingQuery should filter results based on an article type filter") {
    val results = articleSearchService.matchingQuery(
      TestData.testSettings
        .copy(query = Some("bil"), sort = Sort.ByRelevanceDesc, articleTypes = Seq(ArticleType.TopicArticle.entryName))
    )
    results.get.totalCount should be(0)

    val results2 = articleSearchService.matchingQuery(
      TestData.testSettings
        .copy(query = Some("bil"), sort = Sort.ByRelevanceDesc, articleTypes = Seq(ArticleType.Standard.entryName))
    )
    results2.get.totalCount should be(3)
  }

  test("That search matches title and html-content ordered by relevance descending") {
    val results =
      articleSearchService
        .matchingQuery(TestData.testSettings.copy(query = Some("bil"), sort = Sort.ByRelevanceDesc))
        .get
    val hits = results.results
    results.totalCount should be(3)
    hits.map(_.id) should be(Seq(1, 5, 3))
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val results = articleSearchService
      .matchingQuery(TestData.testSettings.copy(query = Some("bil"), withIdIn = List(3), sort = Sort.ByRelevanceDesc))
      .get
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
    hits.last.id should be(3)
  }

  test("That search matches title") {
    val results =
      articleSearchService
        .matchingQuery(TestData.testSettings.copy(query = Some("Pingvinen"), sort = Sort.ByTitleAsc))
        .get
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(2)
  }

  test("That search matches tags") {
    val results =
      articleSearchService.matchingQuery(TestData.testSettings.copy(query = Some("and"), sort = Sort.ByTitleAsc)).get
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val results =
      articleSearchService
        .matchingQuery(TestData.testSettings.copy(query = Some("supermann"), sort = Sort.ByTitleAsc))
        .get
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val results = articleSearchService
      .matchingQuery(
        TestData.testSettings
          .copy(query = Some("supermann"), sort = Sort.ByTitleAsc, license = Some(Copyrighted.toString))
      )
      .get
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(4)
  }

  test("Searching with logical AND only returns results with all terms") {
    val search1 =
      articleSearchService
        .matchingQuery(
          TestData.testSettings.copy(query = Some("bilde + bil"), sort = Sort.ByTitleAsc)
        )
        .get
    val hits1 = search1.results
    hits1.map(_.id) should equal(Seq(1, 3, 5))

    val search2 =
      articleSearchService
        .matchingQuery(
          TestData.testSettings.copy(query = Some("batmen + bil"), sort = Sort.ByTitleAsc)
        )
        .get
    val hits2 = search2.results
    hits2.map(_.id) should equal(Seq(1))

    val search3 = articleSearchService
      .matchingQuery(
        TestData.testSettings.copy(query = Some("bil + bilde + -flaggermusmann"), sort = Sort.ByTitleAsc)
      )
      .get
    val hits3 = search3.results
    hits3.map(_.id) should equal(Seq(3, 5))

    val search4 =
      articleSearchService
        .matchingQuery(
          TestData.testSettings.copy(query = Some("bil + -hulken"), sort = Sort.ByTitleAsc)
        )
        .get
    val hits4 = search4.results
    hits4.map(_.id) should equal(Seq(1, 3))
  }

  test("search in content should be ranked lower than introduction and title") {
    val search = articleSearchService
      .matchingQuery(
        TestData.testSettings.copy(query = Some("mareritt+ragnarok"), sort = Sort.ByRelevanceDesc)
      )
      .get
    val hits = search.results
    hits.map(_.id) should equal(Seq(9, 8))
  }

  test("Search for all languages should return all articles in different languages") {
    val search = articleSearchService
      .matchingQuery(
        TestData.testSettings.copy(language = Language.AllLanguages, pageSize = 100, sort = Sort.ByTitleAsc)
      )
      .get
    search.totalCount should equal(11)
  }

  test("Search for all languages should return all articles in correct language") {
    val search =
      articleSearchService
        .matchingQuery(TestData.testSettings.copy(language = Language.AllLanguages, pageSize = 100))
        .get
    val hits = search.results

    search.totalCount should equal(11)
    hits.head.id should equal(1)
    hits(1).id should equal(2)
    hits(2).id should equal(3)
    hits(3).id should equal(5)
    hits(4).id should equal(6)
    hits(5).id should equal(7)
    hits(6).id should equal(8)
    hits(7).id should equal(9)
    hits(8).id should equal(10)
    hits(9).id should equal(11)
    hits(8).title.language should equal("en")
    hits(9).title.language should equal("nb")
  }

  test("Search for all languages should return all languages if copyrighted") {
    val search = articleSearchService
      .matchingQuery(
        TestData.testSettings.copy(
          language = Language.AllLanguages,
          license = Some(Copyrighted.toString),
          sort = Sort.ByTitleAsc,
          pageSize = 100
        )
      )
      .get
    val hits = search.results

    search.totalCount should equal(1)
    hits.head.id should equal(4)
  }

  test("Searching with query for all languages should return language that matched") {
    val searchEn = articleSearchService
      .matchingQuery(
        TestData.testSettings.copy(query = Some("Cats"), language = Language.AllLanguages, sort = Sort.ByRelevanceDesc)
      )
      .get
    val searchNb = articleSearchService
      .matchingQuery(
        TestData.testSettings
          .copy(query = Some("Katter"), language = Language.AllLanguages, sort = Sort.ByRelevanceDesc)
      )
      .get

    searchEn.totalCount should equal(1)
    searchEn.results.head.id should equal(11)
    searchEn.results.head.title.title should equal("Cats")
    searchEn.results.head.title.language should equal("en")

    searchNb.totalCount should equal(1)
    searchNb.results.head.id should equal(11)
    searchNb.results.head.title.title should equal("Katter")
    searchNb.results.head.title.language should equal("nb")
  }

  test("metadescription is searchable") {
    val search = articleSearchService
      .matchingQuery(
        TestData.testSettings
          .copy(query = Some("hurr dirr"), language = Language.AllLanguages, sort = Sort.ByRelevanceDesc)
      )
      .get

    search.totalCount should equal(1)
    search.results.head.id should equal(11)
    search.results.head.title.title should equal("Cats")
    search.results.head.title.language should equal("en")
  }

  test("That searching with fallback parameter returns article in language priority even if doesnt match on language") {
    val search =
      articleSearchService
        .matchingQuery(
          TestData.testSettings.copy(withIdIn = List(9, 10, 11), language = "en", fallback = true)
        )
        .get

    search.totalCount should equal(3)
    search.results.head.id should equal(9)
    search.results.head.title.language should equal("nb")
    search.results(1).id should equal(10)
    search.results(1).title.language should equal("en")
    search.results(2).id should equal(11)
    search.results(2).title.language should equal("en")
  }

  test("That searching for language not in analyzer works as expected") {
    val search =
      articleSearchService.matchingQuery(TestData.testSettings.copy(language = "biz")).get

    search.totalCount should equal(1)
    search.results.head.id should equal(11)
    search.results.head.title.language should equal("biz")
  }

  test("That searching for language not in index works as expected") {
    val search =
      articleSearchService.matchingQuery(TestData.testSettings.copy(language = "mix")).get

    search.totalCount should equal(0)
  }

  test("That searching for not supported language does not break") {
    val search =
      articleSearchService.matchingQuery(TestData.testSettings.copy(language = "asdf")).get

    search.totalCount should equal(0)
  }

  test("That metaImage altText is included in the search") {
    val search =
      articleSearchService.matchingQuery(TestData.testSettings.copy(withIdIn = List(1), fallback = true)).get
    search.totalCount should be(1)
    search.results.head.metaImage should be(
      Some(
        api.ArticleMetaImageDTO("http://api-gateway.ndla-local/image-api/raw/id/5555", "Alt text is here friend", "nb")
      )
    )
  }

  test("That scrolling works as expected") {
    val pageSize    = 2
    val expectedIds = List(1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13).sliding(pageSize, pageSize).toList

    val initialSearch =
      articleSearchService
        .matchingQuery(
          TestData.testSettings.copy(
            language = Language.AllLanguages,
            pageSize = pageSize,
            fallback = true,
            shouldScroll = true
          )
        )
        .get

    val scroll1 = articleSearchService.scroll(initialSearch.scrollId.get, "*").get
    val scroll2 = articleSearchService.scroll(scroll1.scrollId.get, "*").get
    val scroll3 = articleSearchService.scroll(scroll2.scrollId.get, "*").get
    val scroll4 = articleSearchService.scroll(scroll3.scrollId.get, "*").get
    val scroll5 = articleSearchService.scroll(scroll4.scrollId.get, "*").get

    initialSearch.results.map(_.id) should be(expectedIds.head)
    scroll1.results.map(_.id) should be(expectedIds(1))
    scroll2.results.map(_.id) should be(expectedIds(2))
    scroll3.results.map(_.id) should be(expectedIds(3))
    scroll4.results.map(_.id) should be(expectedIds(4))
    scroll5.results.map(_.id) should be(expectedIds(5))
  }

  test("That highlighting works when scrolling") {
    val initialSearch =
      articleSearchService
        .matchingQuery(
          TestData.testSettings.copy(
            query = Some("about"),
            pageSize = 1,
            fallback = true,
            shouldScroll = true
          )
        )
        .get

    val scroll = articleSearchService.scroll(initialSearch.scrollId.get, "*").get

    initialSearch.results.size should be(1)
    initialSearch.results.head.id should be(10)

    scroll.results.size should be(1)
    scroll.results.head.id should be(11)
    scroll.results.head.title.language should be("en")
    scroll.results.head.title.title should be("Cats")
  }

  test("That filtering for grepCodes works as expected") {

    val search1 = articleSearchService.matchingQuery(TestData.testSettings.copy(grepCodes = Seq("KV123"))).get
    search1.totalCount should be(2)
    search1.results.map(_.id) should be(Seq(1, 2))

    val search2 =
      articleSearchService.matchingQuery(TestData.testSettings.copy(grepCodes = Seq("KV123", "KV456"))).get
    search2.totalCount should be(3)
    search2.results.map(_.id) should be(Seq(1, 2, 3))

    val search3 = articleSearchService.matchingQuery(TestData.testSettings.copy(grepCodes = Seq("KV456"))).get
    search3.totalCount should be(3)
    search3.results.map(_.id) should be(Seq(1, 2, 3))
  }

  test("That 'everyone' doesn't see teacher and student articles in search") {
    val search1 = articleSearchService
      .matchingQuery(
        TestData.testSettings.copy(query = Some("availability"), availability = Seq(Availability.everyone))
      )
      .get

    val search2 =
      articleSearchService
        .matchingQuery(
          TestData.testSettings.copy(query = Some("availability"), availability = Seq.empty)
        )
        .get

    search1.results.map(_.id) should be(Seq(13))
    search2.results.map(_.id) should be(Seq(13))
  }

  test("That 'everyone' doesn't see teacher articles in search") {
    val search1 = articleSearchService
      .matchingQuery(
        TestData.testSettings.copy(query = Some("availability"), availability = Seq(Availability.everyone))
      )
      .get

    search1.results.map(_.id) should be(Seq(13))
  }

  test("That 'teachers' sees teacher articles in search") {
    val search1 = articleSearchService
      .matchingQuery(
        TestData.testSettings
          .copy(query = Some("availability"), availability = Seq(Availability.teacher, Availability.everyone))
      )
      .get

    search1.results.map(_.id) should be(Seq(12, 13))
  }

}
