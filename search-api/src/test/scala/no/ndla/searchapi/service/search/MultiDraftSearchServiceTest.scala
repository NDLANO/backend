/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.{LearningResourceType, MetaImageDTO}
import no.ndla.common.model.domain.ArticleType
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.common.model.domain.learningpath.LearningPathVerificationStatus.CREATED_BY_NDLA
import no.ndla.common.util.TraitUtil
import no.ndla.language.Language.AllLanguages
import no.ndla.mapping.License
import no.ndla.network.tapir.NonEmptyString
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}
import no.ndla.searchapi.SearchTestUtility.*
import no.ndla.searchapi.TestData.*
import no.ndla.searchapi.model.domain.{IndexingBundle, Sort}
import no.ndla.searchapi.model.search.SearchPagination
import no.ndla.searchapi.service.ConverterService
import no.ndla.searchapi.{TestData, TestEnvironment}

import scala.util.Success

class MultiDraftSearchServiceTest extends ElasticsearchIntegrationSuite with TestEnvironment {
  override implicit lazy val e4sClient: NdlaE4sClient =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))
  override implicit lazy val searchLanguage: SearchLanguage                 = new SearchLanguage
  override implicit lazy val converterService: ConverterService             = new ConverterService
  override implicit lazy val traitUtil: TraitUtil                           = new TraitUtil
  override implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService

  override implicit lazy val articleIndexService: ArticleIndexService = new ArticleIndexService {
    override val indexShards = 1
  }
  override implicit lazy val draftIndexService: DraftIndexService = new DraftIndexService {
    override val indexShards = 1
  }
  override implicit lazy val learningPathIndexService: LearningPathIndexService = new LearningPathIndexService {
    override val indexShards = 1
  }
  override implicit lazy val draftConceptIndexService: DraftConceptIndexService = new DraftConceptIndexService {
    override val indexShards = 1
  }
  override implicit lazy val multiDraftSearchService: MultiDraftSearchService = new MultiDraftSearchService {
    override val enableExplanations = true
  }

  val indexingBundle: IndexingBundle =
    IndexingBundle(Some(emptyGrepBundle), Some(taxonomyTestBundle), Some(TestData.myndlaTestBundle))

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (elasticSearchContainer.isSuccess) {
      draftIndexService.createIndexAndAlias()
      learningPathIndexService.createIndexAndAlias()
      draftConceptIndexService.createIndexAndAlias()

      draftsToIndex.map(draft => draftIndexService.indexDocument(draft, indexingBundle))

      learningPathsToIndex.map(lp => learningPathIndexService.indexDocument(lp, indexingBundle))

      blockUntil(() => {
        draftIndexService.countDocuments == draftsToIndex.size &&
        learningPathIndexService.countDocuments == learningPathsToIndex.count(lp =>
          lp.verificationStatus == CREATED_BY_NDLA
        )
      })
    }
  }

  private def expectedAllPublicDrafts(language: String) = {
    val x =
      if (language == "*") {
        draftsToIndex
      } else {
        draftsToIndex.filter(_.title.map(_.language).contains(language))
      }
    x.filterNot(_.status.current == DraftStatus.ARCHIVED).filterNot(_.status.current == DraftStatus.UNPUBLISHED)
  }

  private def expectedAllPublicLearningPaths(language: String) = {
    val x =
      if (language == "*") {
        learningPathsToIndex
      } else {
        learningPathsToIndex.filter(_.title.map(_.language).contains(language))
      }
    x.filter(_.verificationStatus == CREATED_BY_NDLA).filter(_.copyright.license != License.Copyrighted.toString)
  }

  private def idsForLang(language: String) = expectedAllPublicDrafts(language).map(_.id.get) ++
    expectedAllPublicLearningPaths(language).map(_.id.get)

  private def titlesForLang(language: String) = {
    expectedAllPublicDrafts(language).map(_.title.find(_.language == language || language == "*").get.title) ++
      expectedAllPublicLearningPaths(language).map(_.title.find(_.language == language || language == "*").get.title)
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    multiDraftSearchService.getStartAtAndNumResults(0, 10001) should equal(
      Success(SearchPagination(1, props.MaxPageSize, 0))
    )
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page            = 74
    val expectedStartAt = (page - 1) * props.DefaultPageSize
    multiDraftSearchService.getStartAtAndNumResults(page, props.DefaultPageSize) should equal(
      Success(SearchPagination(page, props.DefaultPageSize, expectedStartAt))
    )
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByIdAsc)): @unchecked
    val expected = idsForLang("nb").sorted
    results.totalCount should be(expected.size)
    results.summaryResults.map(_.id) should be(expected)
  }

  test("That all returns all documents ordered by id descending") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByIdDesc)): @unchecked
    val expected = idsForLang("nb").sorted.reverse
    results.totalCount should be(expected.size)
    results.summaryResults.map(_.id) should be(expected)
  }

  test("That all returns all documents ordered by title ascending") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByTitleAsc)): @unchecked
    val expected = titlesForLang("nb").sorted
    results.totalCount should be(expected.size)
    results.summaryResults.map(_.title.title) should be(expected)
  }

  test("That all returns all documents ordered by title descending") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByTitleDesc)): @unchecked
    val expected = titlesForLang("nb").sorted.reverse
    results.totalCount should be(expected.size)
    results.summaryResults.map(_.title.title) should be(expected)
  }

  test("That all returns all documents ordered by lastUpdated descending") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByLastUpdatedDesc)): @unchecked
    val expected = idsForLang("nb")
    results.totalCount should be(expected.size)
    results.summaryResults.head.id should be(4)
    results.summaryResults.last.id should be(5)
  }

  test("That all returns all documents ordered by lastUpdated ascending") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByLastUpdatedAsc)): @unchecked
    val expected = idsForLang("nb")
    results.totalCount should be(expected.size)
    results.summaryResults.head.id should be(5)
    results.summaryResults(1).id should be(1)
    results.summaryResults.last.id should be(4)
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(page1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(page = 1, pageSize = 2, sort = Sort.ByIdAsc)
    ): @unchecked
    val Success(page2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(page = 2, pageSize = 2, sort = Sort.ByIdAsc)
    ): @unchecked
    val expected = idsForLang("nb")
    val hits1    = page1.summaryResults
    val hits2    = page2.summaryResults
    page1.totalCount should be(expected.size)
    page1.page.get should be(1)
    hits1.size should be(2)
    hits1.head.id should be(1)
    hits1.last.id should be(1)
    page2.totalCount should be(expected.size)
    page2.page.get should be(2)
    hits2.size should be(2)
    hits2.head.id should be(2)
    hits2.last.id should be(2)
  }

  test("That search matches title and html-content ordered by relevance descending") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("bil").get), sort = Sort.ByRelevanceDesc)
    ): @unchecked
    results.totalCount should be(3)
    results.summaryResults.map(_.id) should be(Seq(1, 5, 3))
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("bil").get),
        sort = Sort.ByRelevanceDesc,
        withIdIn = List(3),
      )
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(3)
    hits.last.id should be(3)
  }

  test("That search matches title") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("Pingvinen").get), sort = Sort.ByTitleAsc)
    ): @unchecked

    results.summaryResults.map(_.contexts.head.contextType) should be(Seq("learningpath", "standard"))
    results.summaryResults.map(_.id) should be(Seq(1, 2))
    results.totalCount should be(2)
  }

  test("That search matches updatedBy") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(sort = Sort.ByIdAsc, userFilter = List("ndalId54321"))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(13)
    hits.head.id should be(1)
    hits.head.contexts.head.contextType should be("standard")
    hits(1).id should be(2)
  }

  test("That search matches notes") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(sort = Sort.ByIdAsc, userFilter = List("ndalId12345"))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(5)
    hits.head.contexts.head.contextType should be("standard")
  }

  test("That search matches tags") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("and").get), sort = Sort.ByTitleAsc)
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(2)
    hits.head.id should be(3)
    hits(1).id should be(3)
    hits(1).contexts.head.contextType should be("learningpath")
  }

  test("That search returns superman since license is specified as copyrighted") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("supermann").get),
        license = Some(License.Copyrighted.toString),
        sort = Sort.ByTitleAsc,
      )
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(4)
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("bilde + bil").get), sort = Sort.ByTitleAsc)
    ): @unchecked
    val hits1 = search1.summaryResults
    hits1.map(_.id) should equal(Seq(1, 3, 5))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("batmen + bil").get), sort = Sort.ByTitleAsc)
    ): @unchecked
    val hits2 = search2.summaryResults
    hits2.map(_.id) should equal(Seq(1))
  }

  test("That searching with NOT returns expected results") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("-flaggermusmann + (bil + bilde)").get),
        sort = Sort.ByTitleAsc,
      )
    ): @unchecked
    search1.summaryResults.map(_.id) should equal(Seq(3, 5))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("bil + -hulken").get),
        sort = Sort.ByTitleAsc,
      )
    ): @unchecked
    search2.summaryResults.map(_.id) should equal(Seq(1, 3))
  }

  test("search in content should be ranked lower than introduction and title") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("mareritt+ragnarok").get),
        sort = Sort.ByRelevanceDesc,
      )
    ): @unchecked
    val hits = search.summaryResults
    hits.map(_.id) should equal(Seq(9, 8))
  }

  test("Search for all languages should return all articles in different languages") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = AllLanguages, pageSize = 100, sort = Sort.ByTitleAsc)
    ): @unchecked

    search.totalCount should equal(titlesForLang("*").size)
  }

  test("Search for all languages should return all articles in correct language") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = AllLanguages, pageSize = 100)
    ): @unchecked
    val hits = search.summaryResults

    search.totalCount should equal(idsForLang("*").size)
    hits.head.id should be(1)
    hits(1).id should be(1)
    hits(2).id should be(2)
    hits(3).id should be(2)
    hits(4).id should be(3)
    hits(5).id should be(3)
    hits(6).id should be(4)
    hits(7).id should be(4)
    hits(8).id should be(5)
    hits(9).id should be(5)
    hits(9).title.language should be("en")
    hits(10).id should be(6)
    hits(11).id should be(6)
    hits(12).id should be(7)
    hits(13).id should be(8)
    hits(14).id should be(9)
    hits(15).id should be(10)
    hits(15).title.language should be("en")
    hits(16).id should be(11)
    hits(16).title.language should be("nb")
  }

  test("Search for all languages should return all languages if copyrighted") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        language = AllLanguages,
        license = Some(License.Copyrighted.toString),
        pageSize = 100,
        sort = Sort.ByTitleAsc,
      )
    ): @unchecked
    val hits = search.summaryResults

    search.totalCount should equal(1)
    hits.head.id should equal(4)
  }

  test("Searching with query for all languages should return language that matched") {
    val Success(searchEn) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("Cats").get),
        language = AllLanguages,
        sort = Sort.ByRelevanceDesc,
      )
    ): @unchecked
    val Success(searchNb) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("Katter").get),
        language = AllLanguages,
        sort = Sort.ByRelevanceDesc,
      )
    ): @unchecked

    searchEn.totalCount should equal(1)
    searchEn.summaryResults.head.id should equal(11)
    searchEn.summaryResults.head.title.title should equal("Cats")
    searchEn.summaryResults.head.title.language should equal("en")

    searchNb.totalCount should equal(1)
    searchNb.summaryResults.head.id should equal(11)
    searchNb.summaryResults.head.title.title should equal("Katter")
    searchNb.summaryResults.head.title.language should equal("nb")
  }

  test("Searching with query for unknown language should return nothing") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "mix", sort = Sort.ByRelevanceDesc)
    ): @unchecked

    search.totalCount should equal(0)
  }

  test("metadescription is searchable") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("hurr dirr").get),
        language = AllLanguages,
        sort = Sort.ByRelevanceDesc,
      )
    ): @unchecked

    search.totalCount should equal(1)
    search.summaryResults.head.id should equal(11)
    search.summaryResults.head.title.title should equal("Cats")
    search.summaryResults.head.title.language should equal("en")
  }

  test("That searching with fallback parameter returns article in language priority even if doesnt match on language") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(fallback = true, language = "en", withIdIn = List(9, 10, 11))
    ): @unchecked

    search.totalCount should equal(3)
    search.summaryResults.head.id should equal(9)
    search.summaryResults.head.title.language should equal("nb")
    search.summaryResults(1).id should equal(10)
    search.summaryResults(1).title.language should equal("en")
    search.summaryResults(2).id should equal(11)
    search.summaryResults(2).title.language should equal("en")
  }

  test("That filtering for subjects works as expected") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", subjects = List("urn:subject:2"))
    ): @unchecked
    search.totalCount should be(7)
    search.summaryResults.map(_.id) should be(Seq(1, 5, 5, 6, 7, 11, 12))
  }

  test("That filtering for subjects with inactive contexts works as expected") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", subjects = List("urn:subject:2"), filterInactive = true)
    ): @unchecked
    search.totalCount should be(4)
    search.summaryResults.flatMap(_.contexts).toList.length should be(5)
    search.summaryResults.map(_.id) should be(Seq(5, 6, 11, 12))
  }

  test("That filtering for subjects returns all drafts with any of listed subjects") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(subjects = List("urn:subject:2", "urn:subject:1"))
    ): @unchecked
    search.totalCount should be(15)
    search.summaryResults.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 7, 8, 9, 11, 12))
  }

  test("That filtering for invisible subjects returns all drafts with any of listed subjects") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(subjects = List("urn:subject:3"))): @unchecked
    search.totalCount should be(2)
    search.summaryResults.map(_.id) should be(Seq(1, 15))
  }

  test("That filtering for resource-types works as expected") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(resourceTypes = List("urn:resourcetype:academicArticle"))
    ): @unchecked
    search.totalCount should be(2)
    search.summaryResults.map(_.id) should be(Seq(2, 5))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(resourceTypes = List("urn:resourcetype:subjectMaterial"))
    ): @unchecked
    search2.totalCount should be(8)
    search2.summaryResults.map(_.id) should be(Seq(1, 2, 3, 4, 5, 6, 7, 12))

    val Success(search3) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(resourceTypes = List("urn:resourcetype:learningpath"))
    ): @unchecked
    search3.totalCount should be(4)
    search3.summaryResults.map(_.id) should be(Seq(1, 2, 3, 4))
  }

  test("That filtering on multiple context-types returns every selected type") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        language = "*",
        learningResourceTypes = List(LearningResourceType.Article, LearningResourceType.TopicArticle),
      )
    ): @unchecked

    search.summaryResults.map(_.id) should be(Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 15))
    search.totalCount should be(14)
  }

  test("That filtering out inactive contexts works as expected") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", filterInactive = false)
    ): @unchecked

    val totalCount   = search.totalCount
    val ids          = search.summaryResults.map(_.id).length
    val contextCount = search.summaryResults.flatMap(_.contexts).toList.length

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", filterInactive = true)
    ): @unchecked

    totalCount should be > search2.totalCount
    ids should be > search2.summaryResults.map(_.id).length
    contextCount should be > search2.summaryResults.flatMap(_.contexts).toList.length
  }

  test("That filtering on learning-resource-type works") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", learningResourceTypes = List(LearningResourceType.Article))
    ): @unchecked
    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", learningResourceTypes = List(LearningResourceType.TopicArticle))
    ): @unchecked

    search.totalCount should be(8)
    search.summaryResults.map(_.id) should be(Seq(1, 2, 3, 4, 5, 6, 7, 12))

    search2.totalCount should be(6)
    search2.summaryResults.map(_.id) should be(Seq(8, 9, 10, 11, 13, 15))
  }

  test("That filtering on article-type works") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", articleTypes = List(ArticleType.Standard.entryName))
    ): @unchecked
    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", articleTypes = List(ArticleType.TopicArticle.entryName))
    ): @unchecked
    val Success(search3) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", articleTypes = List(ArticleType.FrontpageArticle.entryName))
    ): @unchecked

    search.totalCount should be(8)
    search.summaryResults.map(_.id) should be(Seq(1, 2, 3, 4, 5, 6, 7, 12))

    search2.totalCount should be(6)
    search2.summaryResults.map(_.id) should be(Seq(8, 9, 10, 11, 13, 15))

    search3.totalCount should be(1)
    search3.summaryResults.map(_.id) should be(Seq(16))
  }

  test("That filtering on learningpath learningresourcetype returns learningpaths") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", learningResourceTypes = List(LearningResourceType.LearningPath))
    ): @unchecked

    search.totalCount should be(6)
    search.summaryResults.map(_.id) should be(Seq(1, 2, 3, 4, 5, 6))
    search.summaryResults.map(_.url.contains("learningpath")).distinct should be(Seq(true))
  }

  test("That filtering on supportedLanguages works") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", supportedLanguages = List("en"))
    ): @unchecked
    search.totalCount should be(9)
    search.summaryResults.map(_.id) should be(Seq(2, 3, 4, 5, 6, 10, 11, 13, 15))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", supportedLanguages = List("en", "nb"), pageSize = 100)
    ): @unchecked
    search2.totalCount should be(21)
    search2.summaryResults.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8, 9, 10, 11, 12, 13, 15, 16))

    val Success(search3) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", supportedLanguages = List("nb"))
    ): @unchecked
    search3.totalCount should be(18)
    search3.summaryResults.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 7, 8, 9, 11, 12, 13, 15, 16))
  }

  test("That filtering on supportedLanguages should still prioritize the selected language") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "nb", supportedLanguages = List("en"))
    ): @unchecked

    search.totalCount should be(6)
    search.summaryResults.map(_.id) should be(Seq(2, 3, 4, 11, 13, 15))
    search.summaryResults.map(_.title.language) should be(Seq("nb", "nb", "nb", "nb", "nb", "nb"))
  }

  test("That meta image are returned when searching") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "en", withIdIn = List(10))
    ): @unchecked

    search.totalCount should be(1)
    search.summaryResults.head.id should be(10)
    search.summaryResults.head.metaImage should be(
      Some(MetaImageDTO("http://api-gateway.ndla-local/image-api/raw/id/123", "alt", "en"))
    )
  }

  test("That search matches notes on drafts, but not on other content") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(noteQuery = Some(NonEmptyString.fromString("kakemonster").get))
    ): @unchecked

    search.totalCount should be(1)
    search.summaryResults.head.id should be(5)

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(noteQuery = Some(NonEmptyString.fromString("Katter").get))
    ): @unchecked

    search2.totalCount should be(0)
  }

  test("That search matches notes on drafts, even if query is regular query") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("kakemonster").get))
    ): @unchecked

    search.totalCount should be(1)
    search.summaryResults.head.id should be(5)
  }

  test("That filtering for topics returns every child learningResource") {
    val Success(search) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(topics = List("urn:topic:1"))): @unchecked

    search.totalCount should be(8)

    search.summaryResults.map(_.id) should be(Seq(1, 1, 2, 2, 4, 4, 9, 12))
  }

  test("That searching for authors works as expected") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("Kjekspolitiet").get),
        language = AllLanguages,
      )
    ): @unchecked
    search1.totalCount should be(1)
    search1.summaryResults.map(_.id) should be(Seq(1))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("Svims").get), language = AllLanguages)
    ): @unchecked
    search2.totalCount should be(2)
    search2.summaryResults.map(_.id) should be(Seq(2, 5))
  }

  test("That filtering by relevance id works when no subject is specified") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = AllLanguages, relevanceIds = List("urn:relevance:core"))
    ): @unchecked
    search1.summaryResults.map(_.id) should be(Seq(1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = AllLanguages, relevanceIds = List("urn:relevance:supplementary"))
    ): @unchecked
    search2.summaryResults.map(_.id) should be(Seq(1, 2, 3, 4, 4, 5, 12, 15))

    val Success(search3) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        language = AllLanguages,
        relevanceIds = List("urn:relevance:supplementary", "urn:relevance:core"),
      )
    ): @unchecked
    search3.summaryResults.map(_.id) should be(Seq(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 9, 10, 11, 12, 15))
  }

  test("That filtering by relevance and subject only returns for relevances in filtered subjects") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        language = AllLanguages,
        subjects = List("urn:subject:2"),
        relevanceIds = List("urn:relevance:core"),
      )
    ): @unchecked

    search1.summaryResults.map(_.id) should be(Seq(1, 5, 6, 7, 11))
  }

  test("That scrolling works as expected") {
    val pageSize = 2
    val ids      = idsForLang("*").sorted.sliding(pageSize, pageSize).toList

    val Success(initialSearch) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = AllLanguages, pageSize = pageSize, shouldScroll = true)
    ): @unchecked

    val Success(scroll1)  = multiDraftSearchService.scroll(initialSearch.scrollId.get, "*"): @unchecked
    val Success(scroll2)  = multiDraftSearchService.scroll(scroll1.scrollId.get, "*"): @unchecked
    val Success(scroll3)  = multiDraftSearchService.scroll(scroll2.scrollId.get, "*"): @unchecked
    val Success(scroll4)  = multiDraftSearchService.scroll(scroll3.scrollId.get, "*"): @unchecked
    val Success(scroll5)  = multiDraftSearchService.scroll(scroll4.scrollId.get, "*"): @unchecked
    val Success(scroll6)  = multiDraftSearchService.scroll(scroll5.scrollId.get, "*"): @unchecked
    val Success(scroll7)  = multiDraftSearchService.scroll(scroll6.scrollId.get, "*"): @unchecked
    val Success(scroll8)  = multiDraftSearchService.scroll(scroll7.scrollId.get, "*"): @unchecked
    val Success(scroll9)  = multiDraftSearchService.scroll(scroll8.scrollId.get, "*"): @unchecked
    val Success(scroll10) = multiDraftSearchService.scroll(scroll9.scrollId.get, "*"): @unchecked
    val Success(scroll11) = multiDraftSearchService.scroll(scroll10.scrollId.get, "*"): @unchecked
    val Success(scroll12) = multiDraftSearchService.scroll(scroll11.scrollId.get, "*"): @unchecked

    initialSearch.summaryResults.map(_.id) should be(ids.head)
    scroll1.summaryResults.map(_.id) should be(ids(1))
    scroll2.summaryResults.map(_.id) should be(ids(2))
    scroll3.summaryResults.map(_.id) should be(ids(3))
    scroll4.summaryResults.map(_.id) should be(ids(4))
    scroll5.summaryResults.map(_.id) should be(ids(5))
    scroll6.summaryResults.map(_.id) should be(ids(6))
    scroll7.summaryResults.map(_.id) should be(ids(7))
    scroll8.summaryResults.map(_.id) should be(ids(8))
    scroll9.summaryResults.map(_.id) should be(ids(9))
    scroll10.summaryResults.map(_.id) should be(ids(10))
    scroll11.summaryResults.map(_.id) should be(List.empty)
    scroll12.summaryResults.map(_.id) should be(List.empty)
  }

  test("Filtering for statuses should only return drafts with the specified statuses") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        language = AllLanguages,
        learningResourceTypes = List(LearningResourceType.Article, LearningResourceType.TopicArticle),
        statusFilter = List(DraftStatus.IN_PROGRESS),
      )
    ): @unchecked
    search1.summaryResults.map(_.id) should be(Seq(10, 11))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        language = AllLanguages,
        learningResourceTypes = List(LearningResourceType.Article, LearningResourceType.TopicArticle),
        statusFilter = List(DraftStatus.IMPORTED),
      )
    ): @unchecked
    search2.summaryResults.map(_.id) should be(Seq())

    val Success(search3) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        language = AllLanguages,
        learningResourceTypes = List(LearningResourceType.Article, LearningResourceType.TopicArticle),
        statusFilter = List(DraftStatus.IMPORTED),
        includeOtherStatuses = true,
      )
    ): @unchecked
    search3.summaryResults.map(_.id) should be(Seq(12))
  }

  test("Filtering for statuses should also filter learningPaths") {
    val expectedArticleIds = List(10, 11).map(_.toLong)
    val expectedIds        = expectedArticleIds.sorted

    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = AllLanguages, statusFilter = List(DraftStatus.IN_PROGRESS))
    ): @unchecked
    search1.summaryResults.map(_.id) should be(expectedIds)

  }

  test("That search matches previous notes on drafts") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("kultgammeltnotat").get))
    ): @unchecked
    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(noteQuery = Some(NonEmptyString.fromString("kultgammeltnotat").get))
    ): @unchecked

    search1.totalCount should be(1)
    search1.summaryResults.head.id should be(5)

    search2.totalCount should be(1)
    search2.summaryResults.head.id should be(5)
  }

  test("That filtering on grepCodes returns articles which has grepCodes") {
    val Success(search1) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(grepCodes = List("K123"))): @unchecked
    val Success(search2) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(grepCodes = List("K456"))): @unchecked
    val Success(search3) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(grepCodes = List("K123", "K456"))): @unchecked

    search1.summaryResults.map(_.id) should be(Seq(1, 2, 3))
    search2.summaryResults.map(_.id) should be(Seq(1, 2, 5))
    search3.summaryResults.map(_.id) should be(Seq(1, 2, 3, 5))
  }

  test("excluded drafts should only be returned if filtered by status") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(statusFilter = List(DraftStatus.ARCHIVED))
    ): @unchecked
    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(statusFilter = List(DraftStatus.UNPUBLISHED))
    ): @unchecked
    val Success(search3) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        withIdIn = List(14, 17), // 14 is archived, 17 is unpublished
        statusFilter = List.empty,
      )
    ): @unchecked

    search1.summaryResults.map(_.id) should be(Seq(14))
    search2.summaryResults.map(_.id) should be(Seq(17))
    search3.summaryResults.map(_.id) should be(Seq.empty)
  }

  test("that search with query returns suggestion for query") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("bil").get),
        language = AllLanguages,
        sort = Sort.ByRelevanceDesc,
      )
    ): @unchecked

    search.totalCount should equal(3)
    search.suggestions.length should equal(2)
    search.suggestions.head.name should be("content")
    search.suggestions.head.suggestions.head.text should equal("bil")
    search.suggestions.last.name should be("title")
    search.suggestions.last.suggestions.head.text should equal("bil")
  }

  test("That compound words are matched when searched wrongly if enabled") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("Helse søster").get),
        language = AllLanguages,
        searchDecompounded = true,
      )
    ): @unchecked

    search1.totalCount should be(1)
    search1.summaryResults.map(_.id) should be(Seq(13))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("Helse søster").get),
        language = "nb",
        searchDecompounded = true,
      )
    ): @unchecked

    search2.totalCount should be(1)
    search2.summaryResults.map(_.id) should be(Seq(13))
  }

  test("That compound words are matched when searched wrongly if disabled") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("Helse søster").get),
        language = AllLanguages,
        searchDecompounded = false,
      )
    ): @unchecked

    search1.totalCount should be(0)
    search1.summaryResults.map(_.id) should be(Seq.empty)

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("Helse søster").get),
        language = "nb",
        searchDecompounded = false,
      )
    ): @unchecked

    search2.totalCount should be(0)
    search2.summaryResults.map(_.id) should be(Seq.empty)
  }

  test("Search query should not be decompounded (only indexed documents)") {
    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("Bilsøster").get), language = AllLanguages)
    ): @unchecked

    search1.totalCount should be(0)
  }

  test("That searches for embed attributes matches") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("Flubber").get), language = AllLanguages)
    ): @unchecked
    search.summaryResults.map(_.id) should be(Seq(12))
  }

  test("That searches for embedResource does not partial match") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(embedResource = List("conc"), embedId = Some("55"))
    ): @unchecked
    results.totalCount should be(0)
  }

  test("That searches for data-resource_id matches") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(embedId = Some("222"))): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That searches on embedId and embedResource matches when using other parameters") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("Ekstra").get),
        embedResource = List("image"),
        embedId = Some("55"),
      )
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That searches on embedResource and embedId doesn't match when other parameters have no hits") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("aaaaaaaaaaaaaaaaaaaaaaaaaaaa").get),
        embedResource = List("concept"),
        embedId = Some("77"),
      )
    ): @unchecked
    results.totalCount should be(0)
  }

  test("That search on embed data-resource matches") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(embedResource = List("brightcove"))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on embed data-url matches") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(embedId = Some("http://test.test"))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as embed data-resource_id matches") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("77").get))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as embed data-resource matches") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("brightcove").get))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as embed data-url with id extracted matches") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("1234").get))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as embed data-videoid matches") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("6369137446112").get))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on query as article id matches") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("11").get))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(11)
  }

  test("That search on embed data-content-id matches") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(embedId = Some("111"))): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on embed id with language filter does only return correct language") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "en", embedId = Some("222"))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(13)
  }

  test("That search on embed id with language filter=all matches") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", embedId = Some("222"))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(2)
    hits.map(_.id) should be(Seq(12, 13))
  }

  test("That search on visual element id matches") {
    val Success(results) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(embedId = Some("333"))): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(12))
  }

  test("That search on meta image url matches ") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = "*", embedId = Some("123"))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(10))
  }

  test("That exact word search works for special characters") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("\"delt-streng\"").get), language = "*")
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(15))
  }

  test("That exact word search works for special characters with escape") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("\"delt\\-streng\"").get), language = "*")
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(15))
  }

  test("That multiple exact words can be searched") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("\"delt!streng\" \"delt?streng\"").get),
        language = "*",
      )
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(13))
  }

  test("That multiple exact words can be searched with + operator") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("\"delt!streng\"+\"delt-streng\"").get),
        language = "*",
      )
    ): @unchecked
    results.totalCount should be(0)
  }

  test("That multiple exact words can be searched with - operator") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("\"delt!streng\"+-\"delt-streng\"").get),
        language = "*",
      )
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(13))
  }

  test("That exact and regular words can be searched with - operator") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("\"delt!streng\"+-Helsesøster").get),
        language = "*",
      )
    ): @unchecked
    results.totalCount should be(0)
  }

  test("That exact and regular words can be searched with + operator") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("\"delt!streng\" + Helsesøster").get),
        language = "*",
      )
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(13))
  }

  test("That exact search on word with spaces matches") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("\"artikkeltekst med fire deler\"").get),
        language = "*",
      )
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.map(_.id) should be(Seq(12))
  }

  test("That searches on embedId and embedResource only returns results with an embed matching both params.") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(language = AllLanguages, embedResource = List("concept"), embedId = Some("222"))
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.id should be(12)
  }

  test("That search on embed id supports embed with multiple id attributes") {
    val Success(search1) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(embedId = Some("test-image.id"))): @unchecked
    val Success(search2) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(embedId = Some("test-image.url"))): @unchecked

    search1.totalCount should be(1)
    search1.summaryResults.head.id should be(12)
    search2.totalCount should be(1)
    search2.summaryResults.head.id should be(12)

  }

  test("That search result has license and lastUpdated data") {
    val Success(results) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(
        query = Some(NonEmptyString.fromString("bil").get),
        sort = Sort.ByRelevanceDesc,
        withIdIn = List(3),
      )
    ): @unchecked
    val hits = results.summaryResults
    results.totalCount should be(1)
    hits.head.lastUpdated should be(a[NDLADate])
    hits.head.license should be(Some(License.PublicDomain.toString))
  }

  test("That filtering on tags works") {
    val Success(search) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(tags = List("fugl"), language = "nb")
    ): @unchecked
    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(tags = List("hulk"), language = "nb")
    ): @unchecked

    search.totalCount should be(2)
    search.summaryResults.map(_.id) should be(Seq(1, 2))

    search2.totalCount should be(1)
  }
}
