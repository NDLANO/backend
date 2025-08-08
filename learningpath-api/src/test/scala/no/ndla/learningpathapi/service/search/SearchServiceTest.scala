/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import no.ndla.common.model.{NDLADate, api as commonApi}
import no.ndla.common.model.domain.learningpath.{
  Description,
  EmbedType,
  EmbedUrl,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningStep,
  LearningpathCopyright,
  StepStatus,
  StepType
}
import no.ndla.common.model.domain.{Author, ContributorType, Tag, Title, learningpath}
import no.ndla.language.Language
import no.ndla.learningpathapi.TestData.searchSettings
import no.ndla.learningpathapi.model.domain.*
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.mapping.License
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn

import scala.util.Success
import no.ndla.common.model.domain.Priority

class SearchServiceTest extends ElasticsearchIntegrationSuite with UnitSuite with TestEnvironment {
  import props.{DefaultPageSize, MaxPageSize}
  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.get)
  override val searchConverterService: SearchConverterService = new SearchConverterService
  override val searchIndexService: SearchIndexService         = new SearchIndexService {
    override val indexShards: Int = 1 // 1 shard for accurate scoring in tests
  }
  override val searchService: SearchService = new SearchService

  val paul: Author                     = Author(ContributorType.Writer, "Truly Weird Rand Paul")
  val license: String                  = License.PublicDomain.toString
  val copyright: LearningpathCopyright = LearningpathCopyright(license, List(paul))

  val DefaultLearningPath: LearningPath = LearningPath(
    id = None,
    revision = None,
    externalId = None,
    isBasedOn = None,
    title = List(),
    description = List(),
    coverPhotoId = None,
    duration = Some(0),
    status = LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = clock.now(),
    lastUpdated = clock.now(),
    tags = List(),
    owner = "owner",
    copyright = copyright,
    isMyNDLAOwner = false,
    responsible = None,
    comments = Seq.empty,
    priority = Priority.Unspecified
  )

  val DefaultLearningStep: LearningStep = LearningStep(
    id = None,
    revision = None,
    externalId = None,
    learningPathId = None,
    seqNo = 0,
    title = List(),
    introduction = List(),
    description = List(),
    embedUrl = List(),
    article = None,
    `type` = StepType.INTRODUCTION,
    license = Some(license),
    status = StepStatus.ACTIVE
  )

  val PenguinId   = 1L
  val BatmanId    = 2L
  val DonaldId    = 3L
  val UnrelatedId = 4L
  val EnglandoId  = 5L
  val BrumleId    = 6L

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (elasticSearchContainer.isSuccess) {
      searchIndexService.createIndexAndAlias().get

      doReturn(commonApi.AuthorDTO(ContributorType.Writer, "En eier"), Nil*)
        .when(converterService)
        .asAuthor(any[NdlaUserName])

      val today      = NDLADate.now()
      val yesterday  = NDLADate.now().minusDays(1)
      val tomorrow   = NDLADate.now().plusDays(1)
      val tomorrowp1 = NDLADate.now().plusDays(2)
      val tomorrowp2 = NDLADate.now().plusDays(3)

      val activeStep = DefaultLearningStep.copy(
        id = Some(1),
        revision = Some(1),
        title = List(Title("Active step", "nb")),
        embedUrl = List(EmbedUrl("https://ndla.no/article/1", "nb", EmbedType.OEmbed))
      )

      val deletedStep = DefaultLearningStep.copy(
        id = Some(2),
        revision = Some(1),
        title = List(Title("Deleted step", "nb")),
        embedUrl = List(EmbedUrl("https://ndla.no/article/2", "nb", EmbedType.OEmbed)),
        status = StepStatus.DELETED
      )

      val thePenguin = DefaultLearningPath.copy(
        id = Some(PenguinId),
        title = List(Title("Pingvinen er en kjeltring", "nb")),
        description = List(Description("Dette handler om fugler", "nb")),
        duration = Some(1),
        created = yesterday,
        lastUpdated = yesterday,
        tags = List(Tag(Seq("superhelt", "kanikkefly"), "nb")),
        learningsteps = Some(List(activeStep))
      )

      val batman = DefaultLearningPath.copy(
        id = Some(BatmanId),
        title = List(Title("Batman er en tøff og morsom helt", "nb"), Title("Batman is a tough guy", "en")),
        description = List(Description("Dette handler om flaggermus, som kan ligne litt på en fugl", "nb")),
        duration = Some(2),
        created = yesterday,
        lastUpdated = today,
        tags = List(Tag(Seq("superhelt", "kanfly"), "nb")),
        learningsteps = Some(List(activeStep, deletedStep))
      )

      val theDuck = DefaultLearningPath.copy(
        id = Some(DonaldId),
        title = List(Title("Donald er en tøff, rar og morsom and", "nb"), Title("Donald is a weird duck", "en")),
        description = List(Description("Dette handler om en and, som også minner om både flaggermus og fugler.", "nb")),
        duration = Some(3),
        created = yesterday,
        lastUpdated = tomorrow,
        tags = List(Tag(Seq("disney", "kanfly"), "nb")),
        learningsteps = Some(List(deletedStep)),
        verificationStatus = LearningPathVerificationStatus.CREATED_BY_NDLA
      )

      val unrelated = DefaultLearningPath.copy(
        id = Some(UnrelatedId),
        title = List(Title("Unrelated", "en"), Title("Urelatert", "nb")),
        description = List(Description("This is unrelated", "en"), Description("Dette er en urelatert", "nb")),
        duration = Some(4),
        created = yesterday,
        lastUpdated = tomorrowp1,
        tags = List()
      )

      val englando = DefaultLearningPath.copy(
        id = Some(EnglandoId),
        title = List(Title("Englando", "en"), Title("Djinba", "djb")),
        description = List(Description("This is a englando learningpath", "en"), Description("This is djinba", "djb")),
        duration = Some(5),
        created = yesterday,
        lastUpdated = tomorrowp2,
        tags = List()
      )

      val brumle = DefaultLearningPath.copy(
        id = Some(BrumleId),
        title = List(Title("Brumle", "nb")),
        description = List(Description("Dette er brumle", "nb")),
        duration = Some(5),
        created = yesterday,
        lastUpdated = tomorrowp2,
        tags = List(),
        status = LearningPathStatus.UNLISTED
      )

      searchIndexService.indexDocument(thePenguin).get
      searchIndexService.indexDocument(batman).get
      searchIndexService.indexDocument(theDuck).get
      searchIndexService.indexDocument(unrelated).get
      searchIndexService.indexDocument(englando).get
      searchIndexService.indexDocument(brumle).get

      blockUntil(() => searchService.countDocuments() == 6)
    }
  }

  test("all learningpaths should be returned if fallback is enabled in all-search") {
    val Success(res) = searchService.matchingQuery(
      searchSettings.copy(
        language = Some("hurr durr I'm a language"),
        page = Some(1),
        fallback = true,
        sort = Sort.ByIdDesc
      )
    )
    res.results.length should be(res.totalCount)
    res.totalCount should be(5)
  }

  test("no learningpaths should be returned if fallback is disabled with an unsupported language in all-search") {
    val Success(res) = searchService.matchingQuery(
      searchSettings.copy(
        language = Some("hurr durr I'm a language"),
        page = Some(1),
        fallback = false,
        sort = Sort.ByIdDesc
      )
    )
    res.results.length should be(res.totalCount)
    res.totalCount should be(0)
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService.getStartAtAndNumResults(None, None) should equal((0, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService.getStartAtAndNumResults(None, Some(10001)) should equal((0, MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size"
  ) {
    val page            = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    searchService.getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page            = 100
    val pageSize        = 10
    val expectedStartAt = (page - 1) * pageSize
    searchService.getStartAtAndNumResults(Some(page), Some(pageSize)) should equal((expectedStartAt, pageSize))
  }

  test("That all learningpaths are returned ordered by title descending") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByTitleDesc
      )
    )
    val hits = searchResult.results
    searchResult.totalCount should be(4)

    hits.head.id should be(UnrelatedId)
    hits(1).id should be(PenguinId)
    hits(2).id should be(DonaldId)
    hits(3).id should be(BatmanId)

  }

  test("That all learningpaths are returned ordered by title ascending") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByTitleAsc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(BatmanId)
    hits(1).id should be(DonaldId)
    hits(2).id should be(PenguinId)
    hits(3).id should be(UnrelatedId)
  }

  test("That all learningpaths are returned ordered by id descending") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByIdDesc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(UnrelatedId)
    hits(1).id should be(DonaldId)
    hits(2).id should be(BatmanId)
    hits(3).id should be(PenguinId)
  }

  test("That all learningpaths are returned ordered by id ascending") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByIdAsc,
        language = Some("*")
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(5)
    hits.head.id should be(PenguinId)
    hits(1).id should be(BatmanId)
    hits(2).id should be(DonaldId)
    hits(3).id should be(UnrelatedId)
    hits(4).id should be(EnglandoId)
  }

  test("That order by durationDesc orders search result by duration descending") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByDurationDesc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(UnrelatedId)
  }

  test("That order ByDurationAsc orders search result by duration ascending") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByDurationAsc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(PenguinId)
  }

  test("That order ByLastUpdatedDesc orders search result by last updated date descending") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByLastUpdatedDesc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(UnrelatedId)
    hits.last.id should be(PenguinId)
  }

  test("That order ByLastUpdatedAsc orders search result by last updated date ascending") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByLastUpdatedAsc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(PenguinId)
    hits.last.id should be(UnrelatedId)
  }

  test("That all filtered by id only returns learningpaths with the given ids") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        withIdIn = List(1, 2),
        sort = Sort.ByTitleAsc,
        language = Some(Language.AllLanguages)
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(BatmanId)
    hits.last.id should be(PenguinId)
  }

  test("That searching returns matching documents with unmatching language if fallback is enabled") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("Pingvinen"),
        sort = Sort.ByTitleAsc,
        language = Some("en"),
        fallback = true
      )
    )

    searchResult.totalCount should be(1)
  }

  test("That searching returns no matching documents with unmatching language if fallback is disabled ") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("Pingvinen"),
        sort = Sort.ByTitleAsc,
        language = Some("en"),
        fallback = false
      )
    )

    searchResult.totalCount should be(0)
  }

  test("That searching only returns documents matching the query") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("heltene"),
        sort = Sort.ByTitleAsc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        withIdIn = List(3),
        query = Some("morsom"),
        sort = Sort.ByTitleAsc,
        language = Some(Language.AllLanguages)
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(DonaldId)
  }

  test("That searching only returns documents matching the query in the specified language") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("guy"),
        sort = Sort.ByTitleAsc,
        language = Some("en")
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That searching only returns documents matching the query in the specified standard analyzed language") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("djinba"),
        sort = Sort.ByTitleAsc,
        language = Some("djb")
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(EnglandoId)
  }

  test("That searching returns nothing if language is not indexed") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByTitleAsc,
        language = Some("kra")
      )
    )

    searchResult.totalCount should be(0)
  }

  test("That filtering on tag only returns documents where the tag is present") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByTitleAsc,
        taggedWith = Some("superhelt"),
        language = Some("nb")
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(BatmanId)
    hits.last.id should be(PenguinId)
  }

  test(
    "That filtering on tag combined with search only returns documents where the tag is present and the search matches the query"
  ) {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("heltene"),
        taggedWith = Some("kanfly"),
        sort = Sort.ByTitleAsc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That searching and ordering by relevance is returning Donald before Batman when searching for tough weirdos") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("tøff rar"),
        sort = Sort.ByRelevanceDesc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(DonaldId)
    hits.last.id should be(BatmanId)
  }

  test(
    "That searching and ordering by relevance is returning Donald before Batman and the penguin when searching for duck, bat and bird"
  ) {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("and flaggermus fugl"),
        sort = Sort.ByRelevanceDesc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(3)
    hits.toList.head.id should be(DonaldId)
    hits.toList(1).id should be(BatmanId)
    hits.toList(2).id should be(PenguinId)
  }

  test(
    "That searching and ordering by relevance is not returning Penguin when searching for duck, bat and bird, but filtering on kanfly"
  ) {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("and flaggermus fugl"),
        taggedWith = Some("kanfly"),
        sort = Sort.ByRelevanceDesc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(DonaldId)
    hits.last.id should be(BatmanId)
  }

  test("That a search for flaggremsu returns Donald but not Batman if it is misspelled") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("and flaggremsu"),
        sort = Sort.ByRelevanceDesc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(DonaldId)
  }

  test("That searching with logical operators works") {
    val Success(searchResult1) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("kjeltring + batman"),
        sort = Sort.ByRelevanceAsc
      )
    )
    searchResult1.totalCount should be(0)

    val Success(searchResult2) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("tøff + morsom + -and"),
        sort = Sort.ByRelevanceAsc
      )
    )
    val hits2 = searchResult2.results

    searchResult2.totalCount should be(1)
    hits2.head.id should be(BatmanId)

    val Success(searchResult3) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("tøff | morsom | kjeltring"),
        sort = Sort.ByIdAsc
      )
    )
    val hits3 = searchResult3.results

    searchResult3.totalCount should be(3)
    hits3.map(_.id) should be(Seq(PenguinId, BatmanId, DonaldId))
  }

  test("That searching for multiple languages returns result in matched language") {
    val Success(searchNb) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("Urelatert"),
        language = Some(Language.AllLanguages),
        sort = Sort.ByTitleAsc
      )
    )
    val Success(searchEn) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("Unrelated"),
        language = Some(Language.AllLanguages),
        sort = Sort.ByTitleAsc
      )
    )

    searchEn.totalCount should be(1)
    searchEn.results.head.id should be(UnrelatedId)
    searchEn.results.head.title.language should be("en")
    searchEn.results.head.title.title should be("Unrelated")
    searchEn.results.head.description.description should be("This is unrelated")
    searchEn.results.head.description.language should be("en")

    searchNb.totalCount should be(1)
    searchNb.results.head.id should be(UnrelatedId)
    searchNb.results.head.title.language should be("nb")
    searchNb.results.head.title.title should be("Urelatert")
    searchNb.results.head.description.description should be("Dette er en urelatert")
    searchNb.results.head.description.language should be("nb")
  }

  test("That searching for all languages returns multiple languages") {
    val Success(search) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByTitleAsc,
        language = Some(Language.AllLanguages)
      )
    )

    search.totalCount should be(5)
    search.results.head.id should be(BatmanId)
    search.results(1).id should be(DonaldId)
    search.results(2).id should be(EnglandoId)
    search.results(2).title.language should be("en")
    search.results(3).id should be(PenguinId)
    search.results(4).id should be(UnrelatedId)
    search.results(4).title.language should be("nb")
  }

  test("that supportedLanguages are sorted correctly") {
    val Success(search) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("Batman"),
        sort = Sort.ByTitleAsc,
        language = Some(Language.AllLanguages)
      )
    )
    search.results.head.supportedLanguages should be(Seq("nb", "en"))
  }

  test("That searching with fallback still returns searched language if specified") {
    val Success(search) = searchService.matchingQuery(
      searchSettings.copy(
        language = Some("en"),
        fallback = true
      )
    )

    search.totalCount should be(5)
    search.results.head.id should be(PenguinId)
    search.results(1).id should be(BatmanId)
    search.results(2).id should be(DonaldId)
    search.results(3).id should be(UnrelatedId)
    search.results(4).id should be(EnglandoId)

    search.results.map(_.id) should be(Seq(1, 2, 3, 4, 5))
    search.results.map(_.title.language) should be(Seq("nb", "en", "en", "en", "en"))
  }

  test("That scrolling works as expected") {
    val pageSize    = 2
    val expectedIds = List(1, 2, 3, 4, 5).sliding(pageSize, pageSize).toList

    val Success(initialSearch) = searchService.matchingQuery(
      searchSettings.copy(
        language = Some(Language.AllLanguages),
        pageSize = Some(pageSize),
        fallback = true,
        shouldScroll = true
      )
    )

    val Success(scroll1) = searchService.scroll(initialSearch.scrollId.get, "all")
    val Success(scroll2) = searchService.scroll(scroll1.scrollId.get, "all")
    val Success(scroll3) = searchService.scroll(scroll2.scrollId.get, "all")

    initialSearch.results.map(_.id) should be(expectedIds.head)
    scroll1.results.map(_.id) should be(expectedIds(1))
    scroll2.results.map(_.id) should be(expectedIds(2))
    scroll3.results.map(_.id) should be(List.empty)
  }

  test(
    "That search combined with filter by verification status only returns documents matching the query with the given verification status"
  ) {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        query = Some("flaggermus"),
        language = Some(Language.AllLanguages),
        verificationStatus = Some("EXTERNAL"),
        sort = Sort.ByTitleAsc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test(
    "That search combined with filter by verification status only returns documents with the given verification status"
  ) {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        language = Some(Language.AllLanguages),
        verificationStatus = Some("CREATED_BY_NDLA"),
        sort = Sort.ByTitleAsc
      )
    )
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(DonaldId)
  }

  test("That regular searches only includes published learningpaths") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByIdAsc,
        language = Some(Language.AllLanguages)
      )
    )

    searchResult.totalCount should be(5)
    searchResult.results.map(_.id) should be(Seq(1, 2, 3, 4, 5))
  }

  test("That searching for statuses works as expected") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByIdAsc,
        language = Some(Language.AllLanguages),
        status = List(learningpath.LearningPathStatus.PUBLISHED, learningpath.LearningPathStatus.UNLISTED)
      )
    )

    searchResult.totalCount should be(6)
    searchResult.results.map(_.id) should be(Seq(1, 2, 3, 4, 5, 6))

    val Success(searchResult2) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByIdAsc,
        language = Some(Language.AllLanguages),
        status = List(learningpath.LearningPathStatus.UNLISTED)
      )
    )

    searchResult2.totalCount should be(1)
    searchResult2.results.map(_.id) should be(Seq(6))
  }

  test("That searching for step urls only returns active steps") {
    val Success(searchResult) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByIdAsc,
        language = Some(Language.AllLanguages),
        withPaths = List("https://ndla.no/article/1", "https://ndla.no/article/2")
      )
    )

    searchResult.totalCount should be(2)
    searchResult.results.map(_.id) should be(Seq(1, 2))

    val Success(searchResult2) = searchService.matchingQuery(
      searchSettings.copy(
        sort = Sort.ByIdAsc,
        language = Some(Language.AllLanguages),
        withPaths = List("https://ndla.no/article/2")
      )
    )

    searchResult2.totalCount should be(0)
  }

}
