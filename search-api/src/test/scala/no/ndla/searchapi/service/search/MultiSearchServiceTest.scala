/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.{LearningResourceType, MetaImageDTO, SearchTrait}
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.common.model.domain.{ArticleType, Availability}
import no.ndla.language.Language.AllLanguages
import no.ndla.mapping.License
import no.ndla.network.tapir.NonEmptyString
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.searchapi.TestData.*
import no.ndla.searchapi.model.domain.{IndexingBundle, Sort}
import no.ndla.searchapi.model.search.SearchPagination
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.SearchTestUtility.*

import scala.util.Success

class MultiSearchServiceTest extends ElasticsearchIntegrationSuite with UnitSuite with TestEnvironment {
  import props.DefaultPageSize

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))

  override val articleIndexService: ArticleIndexService = new ArticleIndexService {
    override val indexShards = 1
  }
  override val draftIndexService: DraftIndexService = new DraftIndexService {
    override val indexShards = 1
  }
  override val learningPathIndexService: LearningPathIndexService = new LearningPathIndexService {
    override val indexShards = 1
  }
  override val nodeIndexService: NodeIndexService = new NodeIndexService {
    override val indexShards = 1
  }

  override val multiSearchService = new MultiSearchService {
    override val enableExplanations = true
  }
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (elasticSearchContainer.isSuccess) {
      articleIndexService.createIndexAndAlias()
      draftIndexService.createIndexAndAlias()
      learningPathIndexService.createIndexAndAlias()

      articlesToIndex.map(article =>
        articleIndexService.indexDocument(
          article,
          IndexingBundle(
            Some(grepBundle),
            Some(taxonomyTestBundle),
            Some(TestData.myndlaTestBundle)
          )
        )
      )

      learningPathsToIndex.map(lp =>
        learningPathIndexService.indexDocument(
          lp,
          IndexingBundle(
            Some(emptyGrepBundle),
            Some(taxonomyTestBundle),
            Some(TestData.myndlaTestBundle)
          )
        )
      )

      blockUntil(() => {
        articleIndexService.countDocuments == articlesToIndex.size &&
        learningPathIndexService.countDocuments == learningPathsToIndex.size
      })
    }
  }

  def hasTaxonomy(lp: LearningPath): Boolean =
    taxonomyTestBundle.nodes.map(_.contentUri.get).contains(s"urn:learningpath:${lp.id.get}")

  def hasTaxonomy(ar: Article): Boolean =
    taxonomyTestBundle.nodes.map(_.contentUri.get).contains(s"urn:article:${ar.id.get}")

  private def expectedAllPublicArticles(language: String) = {
    val x = if (language == "*") { TestData.articlesToIndex.filter(_.availability == Availability.everyone) }
    else {
      TestData.articlesToIndex.filter(a =>
        a.title.map(_.language).contains(language) && a.availability == Availability.everyone
      )
    }
    x.filter(_.copyright.license != License.Copyrighted.toString)
  }

  private def expectedAllPublicLearningPaths(language: String) = {
    val x = if (language == "*") { TestData.learningPathsToIndex }
    else {
      TestData.learningPathsToIndex.filter(_.title.map(_.language).contains(language))
    }
    x.filter(_.copyright.license != License.Copyrighted.toString)
  }

  private def idsForLang(language: String) =
    expectedAllPublicArticles(language).map(_.id.get) ++
      expectedAllPublicLearningPaths(language).map(_.id.get)

  private def titlesForLang(language: String) = {
    expectedAllPublicArticles(language).map(_.title.find(_.language == language || language == "*").get.title) ++
      expectedAllPublicLearningPaths(language).map(_.title.find(_.language == language || language == "*").get.title)
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    multiSearchService.getStartAtAndNumResults(0, 10001) should equal(
      Success(SearchPagination(1, props.MaxPageSize, 0))
    )
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page            = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    multiSearchService.getStartAtAndNumResults(page, DefaultPageSize) should equal(
      Success(SearchPagination(page, DefaultPageSize, expectedStartAt))
    )
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByIdAsc))

    val hits = results.summaryResults
    results.totalCount should be(idsForLang("nb").size)
    hits.map(_.id) should be(idsForLang("nb").sorted)
  }

  test("That all returns all documents ordered by id descending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByIdDesc))

    val hits = results.summaryResults
    results.totalCount should be(idsForLang("nb").size)
    hits.map(_.id) should be(idsForLang("nb").sorted.reverse)
  }

  test("That all returns all documents ordered by title ascending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByTitleAsc))
    val hits             = results.summaryResults
    results.totalCount should be(titlesForLang("nb").size)
    hits.map(_.title.title) should be(titlesForLang("nb").sorted)
  }

  test("That all returns all documents ordered by title descending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByTitleDesc))
    val hits             = results.summaryResults
    results.totalCount should be(titlesForLang("nb").size)
    hits.map(_.title.title) should be(titlesForLang("nb").sorted.reverse)
  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByLastUpdatedDesc))
    val hits             = results.summaryResults
    results.totalCount should be(idsForLang("nb").size)
    hits.head.id should be(3)
    hits.last.id should be(5)
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val Success(results) = multiSearchService.matchingQuery(searchSettings.copy(sort = Sort.ByLastUpdatedAsc))
    val hits             = results.summaryResults
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
    val hits1 = page1.summaryResults
    val hits2 = page2.summaryResults
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
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("bil").get), sort = Sort.ByRelevanceDesc)
      )
    val hits = results.summaryResults
    results.totalCount should be(3)
    hits.map(_.id) should be(Seq(1, 5, 3))
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some(NonEmptyString.fromString("bil").get), sort = Sort.ByRelevanceDesc, withIdIn = List(3))
      )
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(3)
    hits.last.id should be(3)
  }

  test("That search matches title") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some(NonEmptyString.fromString("Pingvinen").get), sort = Sort.ByTitleAsc)
      )
    val hits = results.summaryResults
    hits.map(_.contexts.head.contextType) should be(Seq("learningpath", "standard"))
    hits.map(_.id) should be(Seq(1, 2))
  }

  test("That search matches tags") {
    val Success(results) = multiSearchService.matchingQuery(
      searchSettings.copy(Some(NonEmptyString.fromString("and").get), sort = Sort.ByTitleAsc)
    )
    val hits = results.summaryResults
    results.totalCount should be(2)
    hits.head.id should be(3)
    hits(1).id should be(3)
    hits(1).contexts.head.contextType should be("learningpath")
  }

  test("That search does not return superman since it has license copyrighted and license is not specified") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some(NonEmptyString.fromString("supermann").get), sort = Sort.ByTitleAsc)
      )
    results.totalCount should be(0)
  }

  test("That search returns superman since license is specified as copyrighted") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(
          Some(NonEmptyString.fromString("supermann").get),
          license = Some(License.Copyrighted.toString),
          sort = Sort.ByTitleAsc
        )
      )
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(4)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some(NonEmptyString.fromString("bilde + bil").get), sort = Sort.ByTitleAsc)
      )
    val hits1 = search1.summaryResults
    hits1.map(_.id) should equal(Seq(1, 3, 5))

    val Success(search2) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some(NonEmptyString.fromString("batmen + bil").get), sort = Sort.ByTitleAsc)
      )
    val hits2 = search2.summaryResults
    hits2.map(_.id) should equal(Seq(1))

  }

  test("Searching with NOT returns expected results") {
    val Success(search1) = multiSearchService.matchingQuery(
      searchSettings.copy(Some(NonEmptyString.fromString("bil + bilde + -flaggermusmann").get), sort = Sort.ByTitleAsc)
    )
    search1.summaryResults.map(_.id) should equal(Seq(3, 5))

    val Success(search2) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some(NonEmptyString.fromString("bil + -hulken").get), sort = Sort.ByTitleAsc)
      )
    search2.summaryResults.map(_.id) should equal(Seq(1, 3))
  }

  test("search in content should be ranked lower than introduction and title") {
    val Success(search) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some(NonEmptyString.fromString("mareritt+ragnarok").get), sort = Sort.ByRelevanceDesc)
      )
    val hits = search.summaryResults
    hits.map(_.id) should equal(Seq(9, 8))
  }

  test("Search for all languages should return all articles in different languages") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(language = AllLanguages, pageSize = 100, sort = Sort.ByTitleAsc)
    )

    search.totalCount should equal(titlesForLang("*").size)
  }

  test("Search for all languages should return all articles in correct language") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(language = AllLanguages, pageSize = 100))
    val hits = search.summaryResults

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
        .copy(
          language = AllLanguages,
          license = Some(License.Copyrighted.toString),
          pageSize = 100,
          sort = Sort.ByTitleAsc
        )
    )
    val hits = search.summaryResults

    search.totalCount should equal(1)
    hits.head.id should equal(4)
  }

  test("Searching with query for all languages should return language that matched") {
    val Success(searchEn) = multiSearchService.matchingQuery(
      searchSettings.copy(
        Some(NonEmptyString.fromString("Cats").get),
        language = AllLanguages,
        sort = Sort.ByRelevanceDesc
      )
    )
    val Success(searchNb) = multiSearchService.matchingQuery(
      searchSettings.copy(
        Some(NonEmptyString.fromString("Katter").get),
        language = AllLanguages,
        sort = Sort.ByRelevanceDesc
      )
    )

    searchEn.totalCount should equal(1)
    searchEn.summaryResults.head.id should equal(11)
    searchEn.summaryResults.head.title.title should equal("Cats")
    searchEn.summaryResults.head.title.language should equal("en")

    searchNb.totalCount should equal(7)
    searchNb.summaryResults.head.id should equal(11)
    searchNb.summaryResults.head.title.title should equal("Katter")
    searchNb.summaryResults.head.title.language should equal("nb")
    // ... ignoring rest of the results since they only matched because they have this article id 11 in the context breadcrumb
  }

  test("Searching for unknown language should return nothing") {
    val Success(searchEn) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "mix", sort = Sort.ByRelevanceDesc))

    searchEn.totalCount should equal(0)
  }

  test("metadescription is searchable") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(
        Some(NonEmptyString.fromString("hurr dirr").get),
        language = AllLanguages,
        sort = Sort.ByRelevanceDesc
      )
    )

    search.totalCount should equal(1)
    search.summaryResults.head.id should equal(11)
    search.summaryResults.head.title.title should equal("Cats")
    search.summaryResults.head.title.language should equal("en")
  }

  test("That searching with fallback parameter returns article in language priority even if doesnt match on language") {
    val Success(search) =
      multiSearchService.matchingQuery(
        searchSettings.copy(fallback = true, language = "en", withIdIn = List(9, 10, 11))
      )

    search.totalCount should equal(3)
    search.summaryResults.head.id should equal(9)
    search.summaryResults.head.title.language should equal("nb")
    search.summaryResults(1).id should equal(10)
    search.summaryResults(1).title.language should equal("en")
    search.summaryResults(2).id should equal(11)
    search.summaryResults(2).title.language should equal("en")
  }

  test("That filtering for subjects works as expected") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "*", subjects = List("urn:subject:2")))
    search.totalCount should be(7)
    search.summaryResults.head.contexts.length should be(2)
    search.summaryResults.head.contexts
      .map(_.rootId) should be(List("urn:subject:1", "urn:subject:2")) // urn:subject:3 is not visible
    search.summaryResults.map(_.id) should be(Seq(1, 5, 5, 6, 7, 11, 12))
  }

  test("That filtering for subjects returns all resources with any of listed subjects") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(subjects = List("urn:subject:2", "urn:subject:1")))
    search.totalCount should be(14)
    search.summaryResults.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 8, 9, 11, 12))
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
    search.summaryResults.map(_.id) should be(Seq(2, 5))

    val Success(search2) =
      multiSearchService.matchingQuery(searchSettings.copy(resourceTypes = List("urn:resourcetype:subjectMaterial")))
    search2.totalCount should be(7)
    search2.summaryResults.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))

    val Success(search3) =
      multiSearchService.matchingQuery(searchSettings.copy(resourceTypes = List("urn:resourcetype:learningpath")))
    search3.totalCount should be(4)
    search3.summaryResults.map(_.id) should be(Seq(1, 2, 3, 4))
  }

  test("That filtering for multiple resource-types returns resources from both") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(resourceTypes = List("urn:resourcetype:subjectMaterial", "urn:resourcetype:reviewResource"))
    )
    search.totalCount should be(7)
    search.summaryResults.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))
  }

  test("That filtering on learning-resource-type works") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(language = "*", learningResourceTypes = List(LearningResourceType.Article))
    )
    val Success(search2) = multiSearchService.matchingQuery(
      searchSettings.copy(language = "*", learningResourceTypes = List(LearningResourceType.TopicArticle))
    )

    search.totalCount should be(7)
    search.summaryResults.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))

    search2.totalCount should be(4)
    search2.summaryResults.map(_.id) should be(Seq(8, 9, 10, 11))
  }

  test("That filtering on article-type works") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(language = "*", articleTypes = List(ArticleType.Standard.entryName))
    )
    val Success(search2) = multiSearchService.matchingQuery(
      searchSettings.copy(language = "*", articleTypes = List(ArticleType.TopicArticle.entryName))
    )
    val Success(search3) = multiSearchService.matchingQuery(
      searchSettings.copy(language = "*", articleTypes = List(ArticleType.FrontpageArticle.entryName))
    )

    search.totalCount should be(7)
    search.summaryResults.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 12))

    search2.totalCount should be(4)
    search2.summaryResults.map(_.id) should be(Seq(8, 9, 10, 11))

    search3.totalCount should be(1)
    search3.summaryResults.map(_.id) should be(Seq(14))
  }

  test("That filtering on multiple context-types returns every type") {
    val Success(search) =
      multiSearchService.matchingQuery(
        searchSettings.copy(
          language = "*",
          learningResourceTypes = List(LearningResourceType.Article, LearningResourceType.TopicArticle)
        )
      )

    search.totalCount should be(11)
    search.summaryResults.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12))
  }

  test("That filtering on learningpath learningresourcetype returns learningpaths") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(language = "*", learningResourceTypes = List(LearningResourceType.LearningPath))
    )

    search.totalCount should be(6)
    search.summaryResults.map(_.id) should be(Seq(1, 2, 3, 4, 5, 6))
    search.summaryResults.filter(_.contexts.nonEmpty).map(_.contexts.head.contextType) should be(
      // only 5 learningpaths with contexts
      Seq.fill(5) { LearningResourceType.LearningPath.toString }
    )
  }

  test("That filtering out inactive contexts works as expected") {
    val Success(search) = multiSearchService.matchingQuery(
      searchSettings.copy(
        language = "*",
        filterInactive = false
      )
    )

    val totalCount   = search.totalCount
    val ids          = search.summaryResults.map(_.id).length
    val contextCount = search.summaryResults.flatMap(_.contexts).toList.length

    val Success(search2) = multiSearchService.matchingQuery(
      searchSettings.copy(
        language = "*",
        filterInactive = true
      )
    )

    totalCount should be > search2.totalCount
    ids should be > search2.summaryResults.map(_.id).length
    contextCount should be > search2.summaryResults.flatMap(_.contexts).toList.length
  }

  test("That filtering on supportedLanguages works") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "*", supportedLanguages = List("en")))
    search.totalCount should be(8)
    search.summaryResults.map(_.id) should be(Seq(2, 3, 4, 5, 6, 10, 11, 12))

    val Success(search2) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "*", supportedLanguages = List("en", "nb")))
    search2.totalCount should be(18)
    search2.summaryResults.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 6, 7, 8, 9, 10, 11, 12, 14))

    val Success(search3) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "*", supportedLanguages = List("nb")))
    search3.totalCount should be(15)
    search3.summaryResults.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 8, 9, 11, 12, 14))
  }

  test("That filtering on supportedLanguages should still prioritize the selected language") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "nb", supportedLanguages = List("en")))

    search.totalCount should be(5)
    search.summaryResults.map(_.id) should be(Seq(2, 3, 4, 11, 12))
    search.summaryResults.map(_.title.language) should be(Seq("nb", "nb", "nb", "nb", "nb"))
  }

  test("That meta image are returned when searching") {
    val Success(search) = multiSearchService.matchingQuery(searchSettings.copy(language = "en", withIdIn = List(10)))

    search.totalCount should be(1)
    search.summaryResults.head.id should be(10)
    search.summaryResults.head.metaImage should be(
      Some(MetaImageDTO("http://api-gateway.ndla-local/image-api/raw/id/442", "alt", "en"))
    )
  }

  test("That searching for authors works as expected") {
    val Success(search1) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some(NonEmptyString.fromString("Kjekspolitiet").get), language = AllLanguages)
      )
    search1.totalCount should be(1)
    search1.summaryResults.map(_.id) should be(Seq(1))

    val Success(search2) =
      multiSearchService.matchingQuery(
        searchSettings.copy(Some(NonEmptyString.fromString("Svims").get), language = AllLanguages)
      )
    search2.totalCount should be(2)
    search2.summaryResults.map(_.id) should be(Seq(2, 5))
  }

  test("That filtering by relevance id makes sense (with and without subject/filter)") {
    val Success(search1) = multiSearchService.matchingQuery(
      searchSettings.copy(language = AllLanguages, relevanceIds = List("urn:relevance:core"))
    )
    search1.summaryResults.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12))

    val Success(search2) = multiSearchService.matchingQuery(
      searchSettings.copy(language = AllLanguages, relevanceIds = List("urn:relevance:supplementary"))
    )
    search2.summaryResults.map(_.id) should be(Seq(1, 2, 3, 4, 5, 12))

    val Success(search3) = multiSearchService.matchingQuery(
      searchSettings.copy(
        language = AllLanguages,
        relevanceIds = List("urn:relevance:supplementary", "urn:relevance:core")
      )
    )
    search3.summaryResults.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 7, 8, 9, 10, 11, 12))
  }

  test("That filtering by relevance and subject only returns for relevances in filtered subjects") {
    val Success(search1) = multiSearchService.matchingQuery(
      searchSettings
        .copy(language = AllLanguages, subjects = List("urn:subject:2"), relevanceIds = List("urn:relevance:core"))
    )

    search1.summaryResults.map(_.id) should be(Seq(1, 5, 6, 7, 11))
  }

  test("That scrolling works as expected") {
    val pageSize = 2
    val ids      = idsForLang("*").sorted.sliding(pageSize, pageSize).toList

    val Success(initialSearch) =
      multiSearchService.matchingQuery(
        searchSettings.copy(language = AllLanguages, pageSize = pageSize, shouldScroll = true)
      )

    val Success(scroll1) = multiSearchService.scroll(initialSearch.scrollId.get, "*")
    val Success(scroll2) = multiSearchService.scroll(scroll1.scrollId.get, "*")
    val Success(scroll3) = multiSearchService.scroll(scroll2.scrollId.get, "*")
    val Success(scroll4) = multiSearchService.scroll(scroll3.scrollId.get, "*")
    val Success(scroll5) = multiSearchService.scroll(scroll4.scrollId.get, "*")
    val Success(scroll6) = multiSearchService.scroll(scroll5.scrollId.get, "*")
    val Success(scroll7) = multiSearchService.scroll(scroll6.scrollId.get, "*")
    val Success(scroll8) = multiSearchService.scroll(scroll7.scrollId.get, "*")
    val Success(scroll9) = multiSearchService.scroll(scroll8.scrollId.get, "*")

    initialSearch.summaryResults.map(_.id) should be(ids.head)
    scroll1.summaryResults.map(_.id) should be(ids(1))
    scroll2.summaryResults.map(_.id) should be(ids(2))
    scroll3.summaryResults.map(_.id) should be(ids(3))
    scroll4.summaryResults.map(_.id) should be(ids(4))
    scroll5.summaryResults.map(_.id) should be(ids(5))
    scroll6.summaryResults.map(_.id) should be(ids(6))
    scroll7.summaryResults.map(_.id) should be(ids(7))
    scroll8.summaryResults.map(_.id) should be(ids(8))
    scroll9.summaryResults.map(_.id) should be(List.empty)
  }

  test("That filtering on context-types works") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(resourceTypes = List("urn:resourcetype:academicArticle")))
    val Success(search2) =
      multiSearchService.matchingQuery(searchSettings.copy(resourceTypes = List("urn:resourcetype:movieAndClip")))

    search.totalCount should be(2)
    search.summaryResults.map(_.id) should be(Seq(2, 5))

    search2.totalCount should be(0)
  }

  test("That filtering on grepCodes returns articles which has grepCodes") {
    val Success(search1) = multiSearchService.matchingQuery(searchSettings.copy(grepCodes = List("KM123")))
    val Success(search2) = multiSearchService.matchingQuery(searchSettings.copy(grepCodes = List("KE12")))
    val Success(search3) =
      multiSearchService.matchingQuery(searchSettings.copy(grepCodes = List("KM123", "KE34", "TT2")))

    search1.summaryResults.map(_.id) should be(Seq(1, 2, 3))
    search2.summaryResults.map(_.id) should be(Seq(1, 5))
    search3.summaryResults.map(_.id) should be(Seq(1, 2, 3, 5))
  }

  test("That search for grep text returns articles which has grep texts fetched from grepCodes") {
    val Success(search1) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("\"utforsking og problemløysing\"").get))
      )
    search1.summaryResults.map(_.id) should be(Seq(1, 5))
  }

  test("That search result has traits if content has embeds") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some(NonEmptyString.fromString("Ekstrastoff").get)))
    search.totalCount should be(1)
    search.summaryResults.head.id should be(12)
    search.summaryResults.head.traits should be(List(SearchTrait.H5p))
  }

  test("That search can be filtered by traits") {
    val Success(search) =
      multiSearchService.matchingQuery(searchSettings.copy(traits = List(SearchTrait.H5p)))
    search.totalCount should be(1)
    search.summaryResults.head.id should be(12)
    search.summaryResults.head.traits should be(List(SearchTrait.H5p))
  }

  test("That searches for embed attributes matches") {
    val search =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("Flubber").get), language = "nb")
      )
    search.get.summaryResults.map(_.id) should be(Seq(12))
  }

  test("That compound words are matched when searched wrongly") {
    val Success(search1) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("Helse søster").get), language = AllLanguages)
      )

    search1.summaryResults.map(_.id) should be(Seq(12))
    search1.totalCount should be(1)

    val Success(search2) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("Helse søster").get), language = "nb")
      )

    search2.summaryResults.map(_.id) should be(Seq(12))
    search2.totalCount should be(1)
  }

  test("That filterByNoResourceType works by filtering out every document that does not have resourceTypes") {
    val Success(search1) = multiSearchService.matchingQuery(
      searchSettings.copy(language = AllLanguages, sort = Sort.ByIdAsc, filterByNoResourceType = true)
    )
    search1.summaryResults.map(_.id).sorted should be(Seq(6, 8, 9, 10, 11, 14))
  }

  test("Search query should not be decompounded (only indexed documents)") {
    val Success(search1) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("Bilsøster").get), language = AllLanguages)
      )

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
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That searches on embedId and embedResource matches when using other parameters") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(
          query = Some(NonEmptyString.fromString("Ekstra").get),
          embedResource = List("video"),
          embedId = Some("77")
        )
      )
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That searches on embedResource and embedId doesn't match when other parameters have no hits") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings
          .copy(
            query = Some(NonEmptyString.fromString("query-string-without-match").get),
            embedResource = List("video"),
            embedId = Some("77")
          )
      )
    results.totalCount should be(0)
  }

  test("That search on embed data-resource matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(embedResource = List("video")))
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on embed data-content-id matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(embedId = Some("111")))
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on embed data-url matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(embedId = Some("http://test")))
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as embed data-resource_id matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some(NonEmptyString.fromString("77").get)))
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as embed data-resouce matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some(NonEmptyString.fromString("video").get)))
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as article id matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some(NonEmptyString.fromString("11").get)))
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(11)
  }

  test("That search on query as deleted context id matches") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(query = Some(NonEmptyString.fromString("asdf1255").get)))
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on embed id with language filter does only return correct language") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "en", embedId = Some("222")))
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on embed id with language filter=all matches ") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(language = AllLanguages, embedId = Some("222")))
    val hits = results.summaryResults
    results.totalCount should be(2)
    hits.map(_.id) should be(Seq(11, 12))
  }

  test("That search on visual element id matches ") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(embedId = Some("333")))
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(12))
  }

  test("That search on meta image url matches ") {
    val Success(results) =
      multiSearchService.matchingQuery(searchSettings.copy(language = "*", embedId = Some("442")))
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(10))
  }

  test("That exact word search works for special characters") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("\"delt-streng\"").get), language = "*")
      )
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(12))
  }

  test("That exact word search works for special characters with escape") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("\"delt\\-streng\"").get), language = "*")
      )
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(12))
  }

  test("That multiple exact words can be searched") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(
          query = Some(NonEmptyString.fromString("\"delt!streng\" \"delt?streng\"").get),
          language = "*"
        )
      )
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(11))
  }

  test("That multiple exact words can be searched with + operator") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(
          query = Some(NonEmptyString.fromString("\"delt!streng\"+\"delt-streng\"").get),
          language = "*"
        )
      )
    results.totalCount should be(0)
  }

  test("That multiple exact words can be searched with - operator") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(
          query = Some(NonEmptyString.fromString("\"delt!streng\"+-\"delt-streng\"").get),
          language = "*"
        )
      )
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(11))
  }

  test("That exact and regular words can be searched with - operator") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("\"delt!streng\"+-katt").get), language = "*")
      )
    results.totalCount should be(0)
  }

  test("That exact and regular words can be searched with + operator") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("\"delt!streng\" + katt").get), language = "*")
      )
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(11))
  }

  test("That exact search on word with spaces matches") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(
          query = Some(NonEmptyString.fromString("\"artikkeltekst med fire deler\"").get),
          language = "*"
        )
      )
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(10))
  }

  test("That searches on embedId and embedResource only returns results with an embed matching both params.") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(language = AllLanguages, embedResource = List("concept"), embedId = Some("222"))
      )
    val hits = results.summaryResults
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
    search1.summaryResults.head.id should be(12)
    search2.totalCount should be(1)
    search2.summaryResults.head.id should be(12)

  }

  test("Empty availability filtering returns 'everyone', others works as expected") {
    val Success(search1) =
      multiSearchService.matchingQuery(
        searchSettings.copy(query = Some(NonEmptyString.fromString("utilgjengelig").get), availability = List.empty)
      )
    search1.totalCount should be(0)
    search1.summaryResults.map(_.id) should be(Seq.empty)

    val Success(search2) = multiSearchService.matchingQuery(
      searchSettings.copy(
        query = Some(NonEmptyString.fromString("utilgjengelig").get),
        availability = List(Availability.everyone)
      )
    )
    search2.totalCount should be(0)
    search2.summaryResults.map(_.id) should be(Seq.empty)

    val Success(search3) = multiSearchService.matchingQuery(
      searchSettings.copy(
        query = Some(NonEmptyString.fromString("utilgjengelig").get),
        availability = List(Availability.everyone, Availability.teacher)
      )
    )
    search3.totalCount should be(1)
    search3.summaryResults.map(_.id) should be(Seq(13))
  }

  test("That search result has license and lastUpdated data") {
    val Success(results) =
      multiSearchService.matchingQuery(
        searchSettings.copy(
          query = Some(NonEmptyString.fromString("bil").get),
          sort = Sort.ByRelevanceDesc,
          withIdIn = List(3)
        )
      )
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.lastUpdated should be(a[NDLADate])
    hits.head.license should be(Some(License.PublicDomain.toString))
  }
}
