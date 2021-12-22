/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.searchapi.SearchApiProperties.DefaultPageSize
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.integration.Elastic4sClientFactory
import no.ndla.searchapi.model.api.MetaImage
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.domain.learningpath.LearningPath
import no.ndla.searchapi.model.domain.{Language, Sort}
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.{SearchApiProperties, TestData, TestEnvironment, UnitSuite}
import org.scalatest.Outcome

import java.util.Date
import scala.util.{Failure, Success}

class MultiSearchServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {
  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    elasticSearchContainer match {
      case Failure(ex) =>
        println(s"Elasticsearch container not running, cancelling '${this.getClass.getName}'")
        println(s"Got exception: ${ex.getMessage}")
        ex.printStackTrace()
      case _ =>
    }

    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val articleIndexService = new ArticleIndexService
  override val draftIndexService = new DraftIndexService
  override val learningPathIndexService = new LearningPathIndexService
  override val multiSearchService = new MultiSearchService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (elasticSearchContainer.isSuccess) {
      articleIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.Articles))
      draftIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.Drafts))
      learningPathIndexService.createIndexWithName(SearchApiProperties.SearchIndexes(SearchType.LearningPaths))

      val indexedArticles =
        articlesToIndex.map(article => articleIndexService.indexDocument(article, taxonomyTestBundle, Some(grepBundle)))

      val indexedLearningPaths =
        learningPathsToIndex.map(lp =>
          learningPathIndexService.indexDocument(lp, taxonomyTestBundle, Some(emptyGrepBundle)))

      blockUntil(() => {
        articleIndexService.countDocuments == articlesToIndex.size &&
        learningPathIndexService.countDocuments == learningPathsToIndex.size
      })
    }
  }

  def hasTaxonomy(lp: LearningPath): Boolean =
    (taxonomyTestBundle.resources.map(_.contentUri.get) ++ taxonomyTestBundle.topics.map(_.contentUri.get))
      .contains(s"urn:learningpath:${lp.id.get}")

  def hasTaxonomy(ar: Article): Boolean =
    (taxonomyTestBundle.resources.map(_.contentUri.get) ++ taxonomyTestBundle.topics.map(_.contentUri.get))
      .contains(s"urn:article:${ar.id.get}")

  private def expectedAllPublicArticles(language: String) = {
    val x = if (language == "*") { TestData.articlesToIndex.filter(_.availability == Availability.everyone) } else {
      TestData.articlesToIndex.filter(a =>
        a.title.map(_.language).contains(language) && a.availability == Availability.everyone)
    }
    x.filter(_.copyright.license != "copyrighted")
  }

  private def expectedAllPublicLearningPaths(language: String) = {
    val x = if (language == "*") { TestData.learningPathsToIndex } else {
      TestData.learningPathsToIndex.filter(_.title.map(_.language).contains(language))
    }
    x.filter(_.copyright.license != "copyrighted")
  }

  private def idsForLang(language: String) =
    expectedAllPublicArticles(language).map(_.id.get) ++
      expectedAllPublicLearningPaths(language).map(_.id.get)

  private def titlesForLang(language: String) = {
    expectedAllPublicArticles(language).map(_.title.find(_.language == language || language == "*").get.title) ++
      expectedAllPublicLearningPaths(language).map(_.title.find(_.language == language || language == "*").get.title)
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    multiSearchService.getStartAtAndNumResults(0, 10001) should equal((0, SearchApiProperties.MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    multiSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * DefaultPageSize
    multiSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByIdAsc))

    val hits = results.results
    results.totalCount should be(idsForLang("nb").size)
    hits.map(_.id) should be(idsForLang("nb").sorted)
  }

  test("That all returns all documents ordered by id descending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByIdDesc))

    val hits = results.results
    results.totalCount should be(idsForLang("nb").size)
    hits.map(_.id) should be(idsForLang("nb").sorted.reverse)
  }

  test("That all returns all documents ordered by title ascending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(titlesForLang("nb").size)
    hits.map(_.title.title) should be(titlesForLang("nb").sorted)
  }

  test("That all returns all documents ordered by title descending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByTitleDesc))
    val hits = results.results
    results.totalCount should be(titlesForLang("nb").size)
    hits.map(_.title.title) should be(titlesForLang("nb").sorted.reverse)
  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByLastUpdatedDesc))
    val hits = results.results
    results.totalCount should be(idsForLang("nb").size)
    hits.head.id should be(3)
    hits.last.id should be(5)
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByLastUpdatedAsc))
    val hits = results.results
    results.totalCount should be(idsForLang("nb").size)
    hits.head.id should be(5)
    hits(1).id should be(1)
    hits.last.id should be(3)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(page1) =
      multiSearchService.matchingQuery(searchSettings.copy(page = 1, pageSize = 2, sort = Sort.ByTitleAsc))
    val Success(page2) =
      multiSearchService.matchingQuery(searchSettings.copy(page = 2, pageSize = 2, sort = Sort.ByTitleAsc))
    val hits1 = page1.results
    val hits2 = page2.results
    page1.totalCount should be(idsForLang("nb").size)
    page1.page.get should be(1)
    hits1.size should be(2)
    hits1.head.id should be(8)
    hits1.last.id should be(2)
    page2.totalCount should be(idsForLang("nb").size)
    page2.page.get should be(2)
    hits2.size should be(2)
    hits2.head.id should be(1)
    hits2.last.id should be(3)
  }

  test("That search matches title and html-content ordered by relevance descending") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("bil"), sort = Sort.ByRelevanceDesc))
    val hits = results.results
    results.totalCount should be(3)
    hits.head.id should be(5)
    hits(1).id should be(1)
    hits.last.id should be(3)
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some("bil"), sort = Sort.ByRelevanceDesc, withIdIn = List(3)))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(3)
    hits.last.id should be(3)
  }

  test("That search matches title") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(Some("Pingvinen"), sort = Sort.ByTitleAsc))
    val hits = results.results
    hits.map(_.contexts.head.learningResourceType) should be(Seq("learningpath", "standard"))
    hits.map(_.id) should be(Seq(1, 2))
  }

  test("That search matches tags") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(Some("and"), sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(2)
    hits.head.id should be(3)
    hits(1).id should be(3)
    hits(1).contexts.head.learningResourceType should be("learningpath")
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(Some("supermann"), sort = Sort.ByTitleAsc))
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some("supermann"), license = Some("copyrighted"), sort = Sort.ByTitleAsc))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(4)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) =
      multiSearchService.matchingQuery(searchSettings.copy(Some("bilde + bil"), sort = Sort.ByTitleAsc))
    val hits1 = search1.results
    hits1.map(_.id) should equal(Seq(1, 3, 5))

    val Success(search2) =
      multiSearchService.matchingQuery(searchSettings.copy(Some("batmen + bil"), sort = Sort.ByTitleAsc))
    val hits2 = search2.results
    hits2.map(_.id) should equal(Seq(1))

  }

  test("Searching with NOT returns expected results") {
    val Success(search1) = multiSearchService.matchingQuery(
      searchSettings.copy(Some("bil + bilde + -flaggermusmann"), sort = Sort.ByTitleAsc))
    search1.results.map(_.id) should equal(Seq(3, 5))

    val Success(search2) =
      multiSearchService.matchingQuery(searchSettings.copy(Some("bil + -hulken"), sort = Sort.ByTitleAsc))
    search2.results.map(_.id) should equal(Seq(1, 3))
  }

  test("search in content should be ranked lower than introduction and title") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(Some("mareritt+ragnarok"), sort = Sort.ByRelevanceDesc))
    val hits = search.results
    hits.map(_.id) should equal(Seq(9, 8))
  }

  test("Search for all languages should return all articles in different languages") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(language = Language.AllLanguages, pageSize = 100, sort = Sort.ByTitleAsc))

    search.totalCount should equal(titlesForLang("*").size)
  }

  test("Search for all languages should return all articles in correct language") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(language = Language.AllLanguages, pageSize = 100))
    val hits = search.results

    val exp = titlesForLang("*")

    search.totalCount should equal(exp.size)
    hits.head.id should be(1)
    hits(1).id should be(1)
    hits(2).id should be(2)
    hits(3).id should be(2)
    hits(4).id should be(3)
    hits(5).id should be(3)
    hits(6).id should be(4)
    hits(7).id should be(5)
    hits(8).id should be(5)
    hits(8).title.language should be("en")
    hits(9).id should be(6)
    hits(10).id should be(6)
    hits(11).id should be(7)
    hits(12).id should be(8)
    hits(13).id should be(9)
    hits(13).title.language should be("nb")
    hits(14).id should be(10)
    hits(14).title.language should be("en")
    hits(15).id should be(11)
    hits(15).title.language should be("nb")
  }

  test("Search for all languages should return all languages if copyrighted") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings
        .copy(language = Language.AllLanguages, license = Some("copyrighted"), pageSize = 100, sort = Sort.ByTitleAsc))
    val hits = search.results

    search.totalCount should equal(1)
    hits.head.id should equal(4)
  }

  test("Searching with query for all languages should return language that matched") {
    val Success(searchEn) = multiSearchService.matchingQuery(
      searchSettings.copy(Some("Cats"), language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))
    val Success(searchNb) = multiSearchService.matchingQuery(
      searchSettings.copy(Some("Katter"), language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))

    searchEn.totalCount should equal(1)
    searchEn.results.head.id should equal(11)
    searchEn.results.head.title.title should equal("Cats")
    searchEn.results.head.title.language should equal("en")

    searchNb.totalCount should equal(1)
    searchNb.results.head.id should equal(11)
    searchNb.results.head.title.title should equal("Katter")
    searchNb.results.head.title.language should equal("nb")
  }

  test("Searching for unknown language should return nothing") {
    val Success(searchEn) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "mix", sort = Sort.ByRelevanceDesc))

    searchEn.totalCount should equal(0)
  }

  test("Searching with query for language not in analyzer should return something") {
    val Success(searchEn) = multiSearchService.matchingQuery(
      searchSettings.copy(query = Some("Chhattisgarhi"), language = "hne", sort = Sort.ByRelevanceDesc))

    searchEn.totalCount should equal(1)
    searchEn.results.head.id should equal(11)
    searchEn.results.head.title.title should equal("Chhattisgarhi")
    searchEn.results.head.title.language should equal("hne")
  }

  test("metadescription is searchable") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(Some("hurr dirr"), language = Language.AllLanguages, sort = Sort.ByRelevanceDesc))

    search.totalCount should equal(1)
    search.results.head.id should equal(11)
    search.results.head.title.title should equal("Cats")
    search.results.head.title.language should equal("en")
  }

  test("That searching with fallback parameter returns article in language priority even if doesnt match on language") {
    val Success(search) =
      multiSearchService.matchingQuery(
        searchSettings.copy(fallback = true, language = "en", withIdIn = List(9, 10, 11)))

    search.totalCount should equal(3)
    search.results.head.id should equal(9)
    search.results.head.title.language should equal("nb")
    search.results(1).id should equal(10)
    search.results(1).title.language should equal("en")
    search.results(2).id should equal(11)
    search.results(2).title.language should equal("en")
  }

  test("That filtering for subjects works as expected") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "*", subjects = List("urn:subject:2")))
    search.totalCount should be(7)
    search.results.head.contexts.length should be(2)
    search.results.head.contexts
      .map(_.subjectId) should be(List("urn:subject:1", "urn:subject:2")) // urn:subject:3 is not visible
    search.results.map(_.id) should be(Seq(1, 5, 5, 6, 7, 11, 12))
  }

  test("That filtering for subjects returns all resources with any of listed subjects") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(subjects = List("urn:subject:2", "urn:subject:1")))
    search.totalCount should be(14)
    search.results.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 8, 9, 11, 12))
  }

  test("That filtering for invisible subjects returns nothing") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(subjects = List("urn:subject:3")))
    search.totalCount should be(0)
  }

  test("That filtering for resource-types works as expected") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(resourceTypes = List("urn:resourcetype:academicArticle")))
    search.totalCount should be(2)
    search.results.map(_.id) should be(Seq(2, 5))

    val Success(search2) =
      multiSearchService.matchingQuery(searchSettings.copy(resourceTypes = List("urn:resourcetype:subjectMaterial")))
    search2.totalCount should be(7)
    search2.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))

    val Success(search3) =
      multiSearchService.matchingQuery(searchSettings.copy(resourceTypes = List("urn:resourcetype:learningpath")))
    search3.totalCount should be(4)
    search3.results.map(_.id) should be(Seq(1, 2, 3, 4))
  }

  test("That filtering for multiple resource-types returns resources from both") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(resourceTypes = List("urn:resourcetype:subjectMaterial", "urn:resourcetype:reviewResource")))
    search.totalCount should be(7)
    search.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))
  }

  test("That filtering on learning-resource-type works") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(language = "*", learningResourceTypes = List(LearningResourceType.Article)))
    val Success(search2) = multiSearchService.matchingQuery(
      searchSettings.copy(language = "*", learningResourceTypes = List(LearningResourceType.TopicArticle)))

    search.totalCount should be(7)
    search.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))

    search2.totalCount should be(4)
    search2.results.map(_.id) should be(Seq(8, 9, 10, 11))
  }

  test("That filtering on multiple context-types returns every type") {
    val Success(search) =
      multiSearchService.matchingQuery(
        searchSettings.copy(language = "*",
                            learningResourceTypes =
                              List(LearningResourceType.Article, LearningResourceType.TopicArticle)))

    search.totalCount should be(11)
    search.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12))
  }

  test("That filtering on learningpath learningresourcetype returns learningpaths") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(language = "*", learningResourceTypes = List(LearningResourceType.LearningPath)))

    search.totalCount should be(6)
    search.results.map(_.id) should be(Seq(1, 2, 3, 4, 5, 6))
    search.results.filter(_.contexts.nonEmpty).map(_.contexts.head.learningResourceType) should be(
      Seq.fill(5) { LearningResourceType.LearningPath.toString }
    )
  }

  test("That filtering on supportedLanguages works") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "*", supportedLanguages = List("en")))
    search.totalCount should be(8)
    search.results.map(_.id) should be(Seq(2, 3, 4, 5, 6, 10, 11, 12))

    val Success(search2) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "*", supportedLanguages = List("en", "nb")))
    search2.totalCount should be(17)
    search2.results.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 6, 7, 8, 9, 10, 11, 12))

    val Success(search3) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "*", supportedLanguages = List("nb")))
    search3.totalCount should be(14)
    search3.results.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 8, 9, 11, 12))
  }

  test("That filtering on supportedLanguages should still prioritize the selected language") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "nb", supportedLanguages = List("en")))

    search.totalCount should be(5)
    search.results.map(_.id) should be(Seq(2, 3, 4, 11, 12))
    search.results.map(_.title.language) should be(Seq("nb", "nb", "nb", "nb", "nb"))
  }

  test("That meta image are returned when searching") {
    val Success(search) = multiSearchService.matchingQuery(searchSettings.copy(language = "en", withIdIn = List(10)))

    search.totalCount should be(1)
    search.results.head.id should be(10)
    search.results.head.metaImage should be(
      Some(MetaImage("http://api-gateway.ndla-local/image-api/raw/id/442", "alt", "en")))
  }

  test("That searching for authors works as expected") {
    val Success(search1) =
      multiSearchService.matchingQuery(searchSettings.copy(Some("Kjekspolitiet"), language = Language.AllLanguages))
    search1.totalCount should be(1)
    search1.results.map(_.id) should be(Seq(1))

    val Success(search2) =
      multiSearchService.matchingQuery(searchSettings.copy(Some("Svims"), language = Language.AllLanguages))
    search2.totalCount should be(2)
    search2.results.map(_.id) should be(Seq(2, 5))
  }

  test("That filtering by relevance id makes sense (with and without subject/filter)") {
    val Success(search1) = multiSearchService.matchingQuery(
      searchSettings.copy(language = Language.AllLanguages, relevanceIds = List("urn:relevance:core")))
    search1.results.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12))

    val Success(search2) = multiSearchService.matchingQuery(
      searchSettings.copy(language = Language.AllLanguages, relevanceIds = List("urn:relevance:supplementary")))
    search2.results.map(_.id) should be(Seq(1, 2, 3, 4, 5, 12))

    val Success(search3) = multiSearchService.matchingQuery(
      searchSettings.copy(language = Language.AllLanguages,
                          relevanceIds = List("urn:relevance:supplementary", "urn:relevance:core")))
    search3.results.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 7, 8, 9, 10, 11, 12))
  }

  test("That filtering by relevance and subject only returns for relevances in filtered subjects") {
    val Success(search1) = multiSearchService.matchingQuery(
      searchSettings.copy(language = Language.AllLanguages,
                          subjects = List("urn:subject:2"),
                          relevanceIds = List("urn:relevance:core")))

    search1.results.map(_.id) should be(Seq(1, 5, 6, 7, 11))
  }

  test("That scrolling works as expected") {
    val pageSize = 2
    val ids = idsForLang("*").sorted.sliding(pageSize, pageSize).toList

    val Success(initialSearch) =
      multiSearchService.matchingQuery(
        searchSettings.copy(language = Language.AllLanguages, pageSize = pageSize, shouldScroll = true))

    val Success(scroll1) = multiSearchService.scroll(initialSearch.scrollId.get, "*", fallback = true)
    val Success(scroll2) = multiSearchService.scroll(scroll1.scrollId.get, "*", fallback = true)
    val Success(scroll3) = multiSearchService.scroll(scroll2.scrollId.get, "*", fallback = true)
    val Success(scroll4) = multiSearchService.scroll(scroll3.scrollId.get, "*", fallback = true)
    val Success(scroll5) = multiSearchService.scroll(scroll4.scrollId.get, "*", fallback = true)
    val Success(scroll6) = multiSearchService.scroll(scroll5.scrollId.get, "*", fallback = true)
    val Success(scroll7) = multiSearchService.scroll(scroll6.scrollId.get, "*", fallback = true)
    val Success(scroll8) = multiSearchService.scroll(scroll7.scrollId.get, "*", fallback = true)
    val Success(scroll9) = multiSearchService.scroll(scroll8.scrollId.get, "*", fallback = true)

    initialSearch.results.map(_.id) should be(ids.head)
    scroll1.results.map(_.id) should be(ids(1))
    scroll2.results.map(_.id) should be(ids(2))
    scroll3.results.map(_.id) should be(ids(3))
    scroll4.results.map(_.id) should be(ids(4))
    scroll5.results.map(_.id) should be(ids(5))
    scroll6.results.map(_.id) should be(ids(6))
    scroll7.results.map(_.id) should be(ids(7))
    scroll8.results.map(_.id) should be(ids(8))
    scroll9.results.map(_.id) should be(List.empty)
  }

  test("That filtering on context-types works") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(resourceTypes = List("urn:resourcetype:academicArticle")))
    val Success(search2) =
      multiSearchService.matchingQuery(searchSettings.copy(resourceTypes = List("urn:resourcetype:movieAndClip")))

    search.totalCount should be(2)
    search.results.map(_.id) should be(Seq(2, 5))

    search2.totalCount should be(0)
  }

  test("That filtering on grepCodes returns articles which has grepCodes") {
    val Success(search1) = multiSearchService.matchingQuery(searchSettings.copy(grepCodes = List("KM123")))
    val Success(search2) = multiSearchService.matchingQuery(searchSettings.copy(grepCodes = List("KE12")))
    val Success(search3) =
      multiSearchService.matchingQuery(searchSettings.copy(grepCodes = List("KM123", "KE34", "TT2")))

    search1.results.map(_.id) should be(Seq(1, 2, 3))
    search2.results.map(_.id) should be(Seq(1, 5))
    search3.results.map(_.id) should be(Seq(1, 2, 3, 5))
  }

  test("That search for grep text returns articles which has grep texts fetched from grepCodes") {
    val Success(search1) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("\"utforsking og problemløysing\"")))
    search1.results.map(_.id) should be(Seq(1, 5))
  }

  test("That search result has traits if content has embeds") {
    val Success(search) = multiSearchService.matchingQuery(searchSettings.copy(query = Some("Ekstrastoff")))
    search.totalCount should be(1)
    search.results.head.id should be(12)
    search.results.head.traits should be(List("H5P"))
  }

  test("That searches for embed attributes matches") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("Flubber"), language = "nb"))
    search.results.map(_.id) should be(Seq(12))
  }

  test("That compound words are matched when searched wrongly") {
    val Success(search1) = multiSearchService.matchingQuery(
      searchSettings.copy(query = Some("Helse søster"), language = Language.AllLanguages))

    search1.totalCount should be(1)
    search1.results.map(_.id) should be(Seq(12))

    val Success(search2) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("Helse søster"), language = "nb"))

    search2.totalCount should be(1)
    search2.results.map(_.id) should be(Seq(12))
  }

  test("That filterByNoResourceType works by filtering out every document that does not have resourceTypes") {
    val Success(search1) = multiSearchService.matchingQuery(
      searchSettings.copy(language = Language.AllLanguages, sort = Sort.ByIdAsc, filterByNoResourceType = true))
    search1.results.map(_.id).sorted should be(Seq(6, 8, 9, 10, 11))
  }

  test("Search query should not be decompounded (only indexed documents)") {
    val Success(search1) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("Bilsøster"), language = Language.AllLanguages))

    search1.totalCount should be(0)
  }

  test("That searches for embedResource does not partial match") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(embedResource = List("vid"), embedId = Some("55")))
    results.totalCount should be(0)
  }

  test("That searches for data-resource_id matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(embedId = Some("66")))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That searches on embedId and embedResource matches when using other parameters") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some("Ekstra"), embedResource = List("video"), embedId = Some("77")))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That searches on embedResource and embedId doesn't match when other parameters have no hits") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings
          .copy(query = Some("query-string-without-match"), embedResource = List("video"), embedId = Some("77")))
    results.totalCount should be(0)
  }

  test("That search on embed data-resource matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(embedResource = List("video")))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on embed data-content-id matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(embedId = Some("111")))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on embed data-url matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(embedId = Some("http://test")))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as embed data-resource_id matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("77")))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as embed data-resouce matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("video")))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as article id matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("11")))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(11)
  }

  test("That search on embed id with language filter does only return correct language") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "en", embedId = Some("222")))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on embed id with language filter=all matches ") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(language = Language.AllLanguages, embedId = Some("222")))
    val hits = results.results
    results.totalCount should be(2)
    hits.map(_.id) should be(Seq(11, 12))
  }

  test("That search on visual element id matches ") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(embedId = Some("333")))
    val hits = results.results
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(12))
  }

  test("That search on meta image url matches ") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "*", embedId = Some("442")))
    val hits = results.results
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(10))
  }

  test("That exact word search works for special characters") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("\"delt-streng\""), language = "*"))
    val hits = results.results
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(12))
  }

  test("That exact word search works for special characters with escape") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("\"delt\\-streng\""), language = "*"))
    val hits = results.results
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(12))
  }

  test("That multiple exact words can be searched") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some("\"delt!streng\" \"delt?streng\""), language = "*"))
    val hits = results.results
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(11))
  }

  test("That multiple exact words can be searched with + operator") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some("\"delt!streng\"+\"delt-streng\""), language = "*"))
    val hits = results.results
    results.totalCount should be(0)
  }

  test("That multiple exact words can be searched with - operator") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some("\"delt!streng\"+-\"delt-streng\""), language = "*"))
    val hits = results.results
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(11))
  }

  test("That exact and regular words can be searched with - operator") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("\"delt!streng\"+-katt"), language = "*"))
    val hits = results.results
    results.totalCount should be(0)
  }

  test("That exact and regular words can be searched with + operator") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("\"delt!streng\" + katt"), language = "*"))
    val hits = results.results
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(11))
  }

  test("That exact search on word with spaces matches") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some("\"artikkeltekst med fire deler\""), language = "*"))
    val hits = results.results
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(10))
  }

  test("That searches on embedId and embedResource only returns results with an embed matching both params.") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(language = Language.AllLanguages, embedResource = List("concept"), embedId = Some("222")))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.id should be(11)
  }

  test("That search on embed id supports embed with multiple id attributes") {
    val Success(search1) =
      multiSearchService.matchingQuery(
        searchSettings.copy(embedId = Some("test-id1"))
      )
    val Success(search2) =
      multiSearchService.matchingQuery(
        searchSettings.copy(embedId = Some("http://test"))
      )

    search1.totalCount should be(1)
    search1.results.head.id should be(12)
    search2.totalCount should be(1)
    search2.results.head.id should be(12)

  }

  test("Empty availability filtering returns 'everyone', others works as expected") {
    val Success(search1) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some("utilgjengelig"), availability = List.empty))
    search1.totalCount should be(0)
    search1.results.map(_.id) should be(Seq.empty)

    val Success(search2) = multiSearchService.matchingQuery(
      searchSettings.copy(query = Some("utilgjengelig"), availability = List(Availability.everyone)))
    search2.totalCount should be(0)
    search2.results.map(_.id) should be(Seq.empty)

    val Success(search3) = multiSearchService.matchingQuery(
      searchSettings.copy(query = Some("utilgjengelig"),
                          availability = List(Availability.everyone, Availability.student)))
    search3.totalCount should be(0)
    search3.results.map(_.id) should be(Seq.empty)

    val Success(search4) = multiSearchService.matchingQuery(
      searchSettings.copy(query = Some("utilgjengelig"),
                          availability = List(Availability.everyone, Availability.student, Availability.teacher)))
    search4.totalCount should be(1)
    search4.results.map(_.id) should be(Seq(13))
  }

  test("That search result has license and lastUpdated data") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some("bil"), sort = Sort.ByRelevanceDesc, withIdIn = List(3)))
    val hits = results.results
    results.totalCount should be(1)
    hits.head.lastUpdated should be(a[Date])
    hits.head.license should be(Some("publicdomain"))
  }
}
