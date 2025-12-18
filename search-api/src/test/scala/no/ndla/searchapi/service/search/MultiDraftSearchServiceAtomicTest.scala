/*
 * Part of NDLA search-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticApi.indexInto
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import no.ndla.common.CirceUtil
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.*
import no.ndla.common.model.domain.*
import no.ndla.common.model.domain.concept.{ConceptContent, ConceptType}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.taxonomy.{Node, NodeType, TaxonomyBundle, TaxonomyContext}
import no.ndla.common.util.TraitUtil
import no.ndla.network.tapir.NonEmptyString
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}
import no.ndla.searchapi.SearchTestUtility.*
import no.ndla.searchapi.TestData.*
import no.ndla.searchapi.model.domain.{IndexingBundle, Sort}
import no.ndla.searchapi.service.ConverterService
import no.ndla.searchapi.{TestData, TestEnvironment}

import java.util.UUID
import scala.util.{Success, Try}

class MultiDraftSearchServiceAtomicTest extends ElasticsearchIntegrationSuite with TestEnvironment {
  override implicit lazy val e4sClient: NdlaE4sClient =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))
  override implicit lazy val searchLanguage: SearchLanguage                 = new SearchLanguage
  override implicit lazy val converterService: ConverterService             = new ConverterService
  override implicit lazy val traitUtil: TraitUtil                           = new TraitUtil
  override implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService
  override implicit lazy val articleIndexService: ArticleIndexService       = new ArticleIndexService {
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

  override def beforeEach(): Unit = {
    if (elasticSearchContainer.isSuccess) {
      draftConceptIndexService.createIndexAndAlias().get
      articleIndexService.createIndexAndAlias().get
      draftIndexService.createIndexAndAlias().get
      learningPathIndexService.createIndexAndAlias().get
    }
  }

  override def afterEach(): Unit = {
    if (elasticSearchContainer.isSuccess) {
      articleIndexService.deleteIndexAndAlias()
      draftIndexService.deleteIndexAndAlias()
      learningPathIndexService.deleteIndexAndAlias()
      draftConceptIndexService.deleteIndexAndAlias()
    }
  }

  val indexingBundle: IndexingBundle = IndexingBundle(
    grepBundle = Some(grepBundle),
    taxonomyBundle = Some(taxonomyTestBundle),
    myndlaBundle = Some(TestData.myndlaTestBundle),
  )

  test("That search on embed id supports embed with multiple resources") {
    val draft1 = TestData
      .draft1
      .copy(
        id = Some(1),
        content = Seq(
          ArticleContent(
            s"""<section><div data-type="related-content"><$EmbedTagName data-article-id="3" data-resource="related-content"></$EmbedTagName></div></section>""",
            "nb",
          )
        ),
      )
    val draft2 = TestData
      .draft1
      .copy(
        id = Some(2),
        content = Seq(
          ArticleContent(
            s"""<section><$EmbedTagName data-content-id="3" data-resource="content-link">Test?</$EmbedTagName></section>""",
            "nb",
          )
        ),
      )
    val draft3 = TestData.draft1.copy(id = Some(3))
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 3)

    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(embedId = Some("3"), embedResource = List("content-link"))
    ): @unchecked

    search1.totalCount should be(1)
    search1.summaryResults.map(_.id) should be(List(2))

    val Success(search2) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(embedId = Some("3"), embedResource = List("content-link", "related-content"))
    ): @unchecked

    search2.totalCount should be(2)
    search2.summaryResults.map(_.id) should be(List(1, 2))
  }

  test("That sorting by revision date sorts by the earliest 'needs-revision'") {
    val today     = NDLADate.now().withNano(0)
    val yesterday = today.minusDays(1)
    val tomorrow  = today.plusDays(1)

    val draft1 = TestData
      .draft1
      .copy(
        id = Some(1),
        revisionMeta = Seq(
          RevisionMeta(id = UUID.randomUUID(), today, note = "note", status = RevisionStatus.NeedsRevision),
          RevisionMeta(id = UUID.randomUUID(), tomorrow, note = "note", status = RevisionStatus.NeedsRevision),
          RevisionMeta(id = UUID.randomUUID(), yesterday, note = "note", status = RevisionStatus.Revised),
        ),
      )
    val draft2 = TestData
      .draft1
      .copy(
        id = Some(2),
        revisionMeta = Seq(
          RevisionMeta(id = UUID.randomUUID(), yesterday.minusDays(10), note = "note", status = RevisionStatus.Revised)
        ),
      )
    val draft3 = TestData
      .draft1
      .copy(
        id = Some(3),
        revisionMeta =
          Seq(RevisionMeta(id = UUID.randomUUID(), yesterday, note = "note", status = RevisionStatus.NeedsRevision)),
      )
    val draft4 = TestData.draft1.copy(id = Some(4), revisionMeta = Seq())

    val lp1 = TestData
      .learningPath1
      .copy(
        id = Some(5),
        revisionMeta = Seq(
          RevisionMeta(id = UUID.randomUUID(), today, note = "note", status = RevisionStatus.NeedsRevision),
          RevisionMeta(id = UUID.randomUUID(), tomorrow, note = "note", status = RevisionStatus.NeedsRevision),
          RevisionMeta(id = UUID.randomUUID(), yesterday, note = "note", status = RevisionStatus.Revised),
        ),
      )
    val lp2 = TestData
      .learningPath1
      .copy(
        id = Some(6),
        revisionMeta = Seq(
          RevisionMeta(id = UUID.randomUUID(), yesterday.minusDays(10), note = "note", status = RevisionStatus.Revised)
        ),
      )
    val lp3 = TestData
      .learningPath1
      .copy(
        id = Some(7),
        revisionMeta =
          Seq(RevisionMeta(id = UUID.randomUUID(), yesterday, note = "note", status = RevisionStatus.NeedsRevision)),
      )
    val lp4 = TestData.learningPath1.copy(id = Some(8), revisionMeta = Seq())
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get

    learningPathIndexService.indexDocument(lp1, indexingBundle).get
    learningPathIndexService.indexDocument(lp2, indexingBundle).get
    learningPathIndexService.indexDocument(lp3, indexingBundle).get
    learningPathIndexService.indexDocument(lp4, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4 && learningPathIndexService.countDocuments == 4)

    val Success(search1) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByRevisionDateAsc)): @unchecked

    search1.totalCount should be(8)
    search1.summaryResults.map(_.id) should be(List(3, 7, 1, 5, 2, 4, 6, 8))

    val Success(search2) =
      multiDraftSearchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByRevisionDateDesc)): @unchecked

    search2.totalCount should be(8)
    search2.summaryResults.map(_.id) should be(List(1, 5, 3, 7, 2, 4, 6, 8))
  }

  test("Test that searching for note in revision meta works as expected") {
    val today     = NDLADate.now().withNano(0)
    val yesterday = today.minusDays(1)
    val tomorrow  = today.plusDays(1)

    val draft1 = TestData
      .draft1
      .copy(
        id = Some(1),
        revisionMeta = Seq(
          RevisionMeta(id = UUID.randomUUID(), today, note = "apekatt", status = RevisionStatus.NeedsRevision),
          RevisionMeta(id = UUID.randomUUID(), tomorrow, note = "note", status = RevisionStatus.NeedsRevision),
          RevisionMeta(id = UUID.randomUUID(), yesterday, note = "note", status = RevisionStatus.Revised),
        ),
      )
    val draft2 = TestData
      .draft1
      .copy(
        id = Some(2),
        revisionMeta = Seq(
          RevisionMeta(
            id = UUID.randomUUID(),
            yesterday.minusDays(10),
            note = "kinakål",
            status = RevisionStatus.Revised,
          )
        ),
      )
    val draft3 = TestData
      .draft1
      .copy(
        id = Some(3),
        revisionMeta = Seq(
          RevisionMeta(id = UUID.randomUUID(), yesterday, note = "trylleformel", status = RevisionStatus.NeedsRevision)
        ),
      )
    val draft4 = TestData.draft1.copy(id = Some(4), revisionMeta = Seq())
    val lp1    = TestData
      .learningPath1
      .copy(
        id = Some(5),
        revisionMeta = Seq(
          RevisionMeta(
            id = UUID.randomUUID(),
            yesterday.minusDays(10),
            note = "pudding",
            status = RevisionStatus.Revised,
          )
        ),
      )
    val lp2 = TestData
      .learningPath1
      .copy(
        id = Some(6),
        revisionMeta = Seq(
          RevisionMeta(id = UUID.randomUUID(), yesterday, note = "trylleformel", status = RevisionStatus.NeedsRevision)
        ),
      )
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get
    learningPathIndexService.indexDocument(lp1, indexingBundle).get
    learningPathIndexService.indexDocument(lp2, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4 && learningPathIndexService.countDocuments == 2)

    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("trylleformel").get))
    ): @unchecked

    search1.totalCount should be(2)
    search1.summaryResults.map(_.id) should be(List(3, 6))
  }

  test("Test that filtering revision dates works as expected") {
    val today = NDLADate.now().withNano(0)

    val draft1 = TestData
      .draft1
      .copy(
        id = Some(1),
        revisionMeta = Seq(
          RevisionMeta(
            id = UUID.randomUUID(),
            today.plusDays(1),
            note = "apekatt",
            status = RevisionStatus.NeedsRevision,
          ),
          RevisionMeta(id = UUID.randomUUID(), today.plusDays(10), note = "note", status = RevisionStatus.Revised),
        ),
      )
    val draft2 = TestData
      .draft1
      .copy(
        id = Some(2),
        revisionMeta = Seq(
          RevisionMeta(id = UUID.randomUUID(), today.minusDays(10), note = "kinakål", status = RevisionStatus.Revised)
        ),
      )
    val draft3 = TestData
      .draft1
      .copy(
        id = Some(3),
        revisionMeta = Seq(
          RevisionMeta(
            id = UUID.randomUUID(),
            today.minusDays(10),
            note = "trylleformel",
            status = RevisionStatus.NeedsRevision,
          )
        ),
      )
    val draft4 = TestData.draft1.copy(id = Some(4), revisionMeta = Seq())
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(revisionDateFilterFrom = Some(today), revisionDateFilterTo = None))
      .get
      .summaryResults
      .map(_.id) should be(Seq(1))

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(revisionDateFilterFrom = None, revisionDateFilterTo = Some(today)))
      .get
      .summaryResults
      .map(_.id) should be(Seq(3))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          revisionDateFilterFrom = Some(today.minusDays(11)),
          revisionDateFilterTo = Some(today.plusDays(1)),
        )
      )
      .get
      .summaryResults
      .map(_.id) should be(Seq(1, 3))
  }

  test("That hits from revision log is not included when exclude param is set") {
    val today = NDLADate.now().withNano(0)

    val status = Status(current = DraftStatus.PLANNED, other = Set.empty)
    val mkNote = (n: String) => EditorNote(n, "some-user", status, today)

    val draft1 = TestData
      .draft1
      .copy(
        id = Some(1),
        notes = Seq(mkNote("Katt"), mkNote("Hund")),
        previousVersionsNotes = Seq(mkNote("Tiger"), mkNote("Gris")),
      )
    val draft2 = TestData
      .draft1
      .copy(
        id = Some(2),
        notes = Seq(mkNote("Kinakål"), mkNote("Grevling"), mkNote("Apekatt"), mkNote("Gris")),
        previousVersionsNotes = Seq(mkNote("Giraff")),
      )
    val draft3 = TestData
      .draft1
      .copy(id = Some(3), title = Seq(Title("Gris", "nb")), notes = Seq(), previousVersionsNotes = Seq())
    draftIndexService.indexDocument(draft1, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft2, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft3, indexingBundle).failIfFailure

    blockUntil(() => draftIndexService.countDocuments == 3)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          query = Some(NonEmptyString.fromString("Gris").get),
          excludeRevisionHistory = true,
        )
      )
      .get
      .summaryResults
      .map(_.id) should be(Seq(2, 3))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          query = Some(NonEmptyString.fromString("Gris").get),
          excludeRevisionHistory = false,
        )
      )
      .get
      .summaryResults
      .map(_.id) should be(Seq(1, 2, 3))
  }

  test("That responsibleId is filterable") {
    val draft1 = TestData.draft1.copy(id = Some(1), responsible = Some(Responsible("hei", TestData.today)))
    val draft2 = TestData.draft1.copy(id = Some(2), responsible = Some(Responsible("hei2", TestData.today)))
    val draft3 = TestData.draft1.copy(id = Some(3), responsible = Some(Responsible("hei", TestData.today)))
    draftIndexService.indexDocument(draft1, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft2, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft3, indexingBundle).failIfFailure

    blockUntil(() => draftIndexService.countDocuments == 3)

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(responsibleIdFilter = List.empty))
      .get
      .summaryResults
      .map(_.id) should be(Seq(1, 2, 3))

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(responsibleIdFilter = List("hei")))
      .get
      .summaryResults
      .map(_.id) should be(Seq(1, 3))

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(responsibleIdFilter = List("hei2")))
      .get
      .summaryResults
      .map(_.id) should be(Seq(2))

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(responsibleIdFilter = List("hei", "hei2")))
      .get
      .summaryResults
      .map(_.id) should be(Seq(1, 2, 3))
  }

  test("That responsible lastUpdated is sortable") {
    val draft1 = TestData.draft1.copy(id = Some(1), responsible = Some(Responsible("hei", TestData.today.minusDays(5))))
    val draft2 = TestData
      .draft1
      .copy(id = Some(2), responsible = Some(Responsible("hei2", TestData.today.minusDays(2))))
    val draft3 = TestData.draft1.copy(id = Some(3), responsible = Some(Responsible("hei", TestData.today.minusDays(3))))
    val draft4 = TestData.draft1.copy(id = Some(4), responsible = None)
    draftIndexService.indexDocument(draft1, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft2, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft3, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft4, indexingBundle).failIfFailure

    blockUntil(() => draftIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(responsibleIdFilter = List.empty, sort = Sort.ByResponsibleLastUpdatedAsc)
      )
      .get
      .summaryResults
      .map(_.id) should be(Seq(1, 3, 2, 4))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(responsibleIdFilter = List.empty, sort = Sort.ByResponsibleLastUpdatedDesc)
      )
      .get
      .summaryResults
      .map(_.id) should be(Seq(2, 3, 1, 4))
  }

  test("That primary connections are handled correctly") {
    val draft1 = TestData.draft1.copy(id = Some(1)) // T1
    val draft2 = TestData.draft1.copy(id = Some(2)) // T2
    val draft3 = TestData.draft1.copy(id = Some(3)) // T3
    val draft4 = TestData.draft1.copy(id = Some(4)) // T4
    val draft5 = TestData.draft1.copy(id = Some(5)) // R5 + R6

    val context_1: TaxonomyContext = TaxonomyContext(
      publicId = "urn:subject:1",
      rootId = "urn:subject:1",
      root = SearchableLanguageValues(Seq(LanguageValue("nb", "Matte"))),
      path = "/subject:1",
      breadcrumbs = SearchableLanguageList(Seq(LanguageValue("nb", Seq.empty))),
      contextType = None,
      relevanceId = core.id,
      relevance = SearchableLanguageValues(Seq.empty),
      resourceTypes = List.empty,
      parentIds = List.empty,
      isPrimary = true,
      contextId = "",
      isVisible = true,
      isActive = true,
      isArchived = false,
      url = "/f/matte/asdf1256",
    )
    val subject_1 = Node(
      "urn:subject:1",
      "Matte",
      None,
      Some("/subject:1"),
      Some("/f/matte/asdf1256"),
      visibleMetadata,
      List.empty,
      NodeType.SUBJECT,
      List.empty,
      List("asdf1256"),
      Some(context_1),
      List(context_1),
    )
    val topic_1 = Node(
      "urn:topic:1",
      "T1",
      Some(s"urn:article:${draft1.id.get}"),
      Some("/subject:1/topic:1"),
      Some("/e/t1/asdf1257"),
      visibleMetadata,
      List.empty,
      NodeType.TOPIC,
      List.empty,
      List("asdf1257"),
      None,
      List.empty,
    )
    topic_1.contexts = generateContexts(
      topic_1,
      subject_1,
      subject_1,
      List.empty,
      None,
      core,
      isPrimary = true,
      isVisible = true,
      isActive = true,
    )
    val topic_2 = Node(
      "urn:topic:2",
      "T2",
      Some(s"urn:article:${draft2.id.get}"),
      Some("/subject:1/topic:2"),
      Some("/e/t2/asdf1258"),
      visibleMetadata,
      List.empty,
      NodeType.TOPIC,
      List.empty,
      List("asdf1258"),
      None,
      List.empty,
    )
    topic_2.contexts = generateContexts(
      topic_2,
      subject_1,
      subject_1,
      List.empty,
      None,
      core,
      isPrimary = true,
      isVisible = true,
      isActive = true,
    )
    val topic_3 = Node(
      "urn:topic:3",
      "T3",
      Some(s"urn:article:${draft3.id.get}"),
      Some("/subject:1/topic:1/topic:3"),
      Some("/e/t3/asdf1259"),
      visibleMetadata,
      List.empty,
      NodeType.TOPIC,
      List.empty,
      List("asdf1259"),
      None,
      List.empty,
    )
    topic_3.contexts = generateContexts(
      topic_3,
      subject_1,
      topic_1,
      List.empty,
      None,
      core,
      isPrimary = true,
      isVisible = true,
      isActive = true,
    )
    val topic_4 = Node(
      "urn:topic:4",
      "T4",
      Some(s"urn:article:${draft4.id.get}"),
      Some("/subject:1/topic:1/topic:4"),
      Some("/e/t4/asdf1260"),
      visibleMetadata,
      List.empty,
      NodeType.TOPIC,
      List.empty,
      List("asdf1260"),
      None,
      List.empty,
    )
    topic_4.contexts = generateContexts(
      topic_4,
      subject_1,
      topic_1,
      List.empty,
      None,
      core,
      isPrimary = true,
      isVisible = true,
      isActive = true,
    )
    val resource_5 = Node(
      "urn:resource:5",
      "R5",
      Some(s"urn:article:${draft5.id.get}"),
      Some("/subject:1/topic:1/resource:5"),
      Some("/r/r5/asdf1261"),
      visibleMetadata,
      List.empty,
      NodeType.RESOURCE,
      List.empty,
      List("asdf1261"),
      None,
      List.empty,
    )
    resource_5.contexts = generateContexts(
      resource_5,
      subject_1,
      topic_1,
      List.empty,
      None,
      core,
      isPrimary = true,
      isVisible = true,
      isActive = true,
    ) ++
      generateContexts(
        resource_5,
        subject_1,
        topic_3,
        List.empty,
        None,
        core,
        isPrimary = false,
        isVisible = true,
        isActive = true,
      )
    val resource_6 = Node(
      "urn:resource:6",
      "R6",
      Some(s"urn:article:${draft5.id.get}"),
      Some("/subject:1/topic:1/topic:3/resource:6"),
      Some("/r/r6/asdf1262"),
      visibleMetadata,
      List.empty,
      NodeType.RESOURCE,
      List.empty,
      List("asdf1262"),
      None,
      List.empty,
    )
    resource_6.contexts = generateContexts(
      resource_6,
      subject_1,
      topic_3,
      List.empty,
      None,
      core,
      isPrimary = true,
      isVisible = true,
      isActive = true,
    )

    val taxonomyBundle = {
      TaxonomyBundle(nodes = List(subject_1, topic_1, topic_2, topic_3, topic_4, resource_5, resource_6))
    }

    {
      draftIndexService.indexDocument(draft1, indexingBundle.copy(taxonomyBundle = Some(taxonomyBundle))).failIfFailure
      draftIndexService.indexDocument(draft2, indexingBundle.copy(taxonomyBundle = Some(taxonomyBundle))).failIfFailure
      draftIndexService.indexDocument(draft3, indexingBundle.copy(taxonomyBundle = Some(taxonomyBundle))).failIfFailure
      draftIndexService.indexDocument(draft4, indexingBundle.copy(taxonomyBundle = Some(taxonomyBundle))).failIfFailure
      draftIndexService.indexDocument(draft5, indexingBundle.copy(taxonomyBundle = Some(taxonomyBundle))).failIfFailure
      blockUntil(() => draftIndexService.countDocuments == 5)
    }

    val result = multiDraftSearchService.matchingQuery(multiDraftSearchSettings).get

    def ctxsFor(id: Long): List[ApiTaxonomyContextDTO] = result.summaryResults.find(_.id == id).get.contexts
    def ctxFor(id: Long): ApiTaxonomyContextDTO        = {
      val ctxs = ctxsFor(id)
      ctxs.length should be(1)
      ctxs.head
    }

    ctxFor(1).isPrimary should be(true)
    ctxFor(2).isPrimary should be(true)
    ctxFor(3).isPrimary should be(true)
    ctxFor(4).isPrimary should be(true)
    ctxsFor(5).map(_.isPrimary) should be(Seq(true, true, false)) // Sorted with primary first

  }

  test("That sorting by status works as expected") {
    val draft1 = TestData.draft1.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    val draft2 = TestData.draft1.copy(id = Some(2), status = Status(DraftStatus.PUBLISHED, Set.empty))
    val draft3 = TestData.draft1.copy(id = Some(3), status = Status(DraftStatus.LANGUAGE, Set.empty))
    val draft4 = TestData.draft1.copy(id = Some(4), status = Status(DraftStatus.FOR_APPROVAL, Set.empty))

    draftIndexService.indexDocument(draft1, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft2, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft3, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft4, indexingBundle).failIfFailure

    blockUntil(() => draftIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(responsibleIdFilter = List.empty, sort = Sort.ByStatusAsc))
      .get
      .summaryResults
      .map(_.id) should be(Seq(4, 3, 1, 2))

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(responsibleIdFilter = List.empty, sort = Sort.ByStatusDesc))
      .get
      .summaryResults
      .map(_.id) should be(Seq(2, 1, 3, 4))
  }

  test("Test that filtering prioritized works as expected") {

    val draft1 = TestData.draft1.copy(id = Some(1), priority = Priority.Unspecified)
    val draft2 = TestData.draft1.copy(id = Some(2), priority = Priority.Prioritized)
    val draft3 = TestData.draft1.copy(id = Some(3), priority = Priority.Prioritized)
    val draft4 = TestData.draft1.copy(id = Some(4))
    val draft5 = TestData.draft1.copy(id = Some(5), priority = Priority.OnHold)

    val lp1 = TestData.learningPath1.copy(id = Some(6), priority = Priority.Prioritized)
    val lp2 = TestData.learningPath1.copy(id = Some(7), priority = Priority.Unspecified)
    val lp3 = TestData.learningPath1.copy(id = Some(8), priority = Priority.Prioritized)
    val lp4 = TestData.learningPath1.copy(id = Some(9))
    val lp5 = TestData.learningPath1.copy(id = Some(10), priority = Priority.OnHold)
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get
    draftIndexService.indexDocument(draft5, indexingBundle).get
    learningPathIndexService.indexDocument(lp1, indexingBundle).get
    learningPathIndexService.indexDocument(lp2, indexingBundle).get
    learningPathIndexService.indexDocument(lp3, indexingBundle).get
    learningPathIndexService.indexDocument(lp4, indexingBundle).get
    learningPathIndexService.indexDocument(lp5, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 5 && learningPathIndexService.countDocuments == 5)

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(priority = List(Priority.Prioritized)))
      .get
      .summaryResults
      .map(_.id) should be(Seq(2, 3, 6, 8))

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(priority = List(Priority.Unspecified)))
      .get
      .summaryResults
      .map(_.id) should be(Seq(1, 4, 7, 9))
    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(priority = List(Priority.OnHold)))
      .get
      .summaryResults
      .map(_.id) should be(Seq(5, 10))

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(priority = List.empty))
      .get
      .summaryResults
      .map(_.id) should be(Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
  }

  test("That search on embed id supports video embed with timestamp resources") {
    val videoId = "66772123"
    val draft1  = TestData
      .draft1
      .copy(
        id = Some(1),
        content = Seq(
          ArticleContent(
            s"""<section><div data-type="related-content"><$EmbedTagName data-resource="video" data-videoid="$videoId&amp;t=1"></$EmbedTagName></div></section>""",
            "nb",
          )
        ),
      )
    draftIndexService.indexDocument(draft1, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 1)

    val Success(search1) = multiDraftSearchService.matchingQuery(
      multiDraftSearchSettings.copy(embedId = Some(videoId), embedResource = List("video"))
    ): @unchecked

    search1.totalCount should be(1)
    search1.summaryResults.map(_.id) should be(List(1))
  }

  test("That sorting on resource types works as expected") {
    val indexService = new DraftIndexService {
      override val indexShards = 1
      override def createIndexRequest(
          domainModel: Draft,
          indexName: String,
          indexingBundle: IndexingBundle,
      ): Try[Option[IndexRequest]] = {

        val draft = domainModel.id.get match {
          case 1 => TestData
              .searchableDraft
              .copy(
                id = 1,
                defaultParentTopicName = Some("Apekatt emne"),
                parentTopicName = SearchableLanguageValues.from("nb" -> "Apekatt emne"),
                defaultRoot = Some("Capekatt rot"),
                primaryRoot = SearchableLanguageValues.from("nb" -> "Capekatt rot"),
                resourceTypeName = SearchableLanguageValues.from("nb" -> "Bapekatt ressurs"),
                defaultResourceTypeName = Some("Bapekatt ressurs"),
                nodes = nodes,
              )
          case 2 => TestData
              .searchableDraft
              .copy(
                id = 2,
                defaultParentTopicName = Some("Bpekatt emne"),
                parentTopicName = SearchableLanguageValues.from("nb" -> "Bpekatt emne"),
                defaultRoot = Some("Apekatt rot"),
                primaryRoot = SearchableLanguageValues.from("nb" -> "Apekatt rot"),
                resourceTypeName = SearchableLanguageValues.from("nb" -> "Capekatt ressurs"),
                defaultResourceTypeName = Some("Capekatt ressurs"),
                nodes = nodes,
              )
          case 3 => TestData
              .searchableDraft
              .copy(
                id = 3,
                defaultParentTopicName = Some("Cpekatt emne"),
                parentTopicName = SearchableLanguageValues.from("nb" -> "Cpekatt emne"),
                defaultRoot = Some("Bapekatt rot"),
                primaryRoot = SearchableLanguageValues.from("nb" -> "Bapekatt rot"),
                resourceTypeName = SearchableLanguageValues.from("nb" -> "Apekatt ressurs"),
                defaultResourceTypeName = Some("Apekatt ressurs"),
                nodes = nodes,
              )
          case _ => fail("Unexpected id, this is a bug with the test")
        }

        val source = CirceUtil.toJsonString(draft)
        Success(Some(indexInto(indexName).doc(source).id(domainModel.id.get.toString)))
      }
    }

    indexService.indexDocument(TestData.draft1.copy(id = Some(1)), indexingBundle).get
    indexService.indexDocument(TestData.draft1.copy(id = Some(2)), indexingBundle).get
    indexService.indexDocument(TestData.draft1.copy(id = Some(3)), indexingBundle).get

    blockUntil(() => indexService.countDocuments == 3)
    val searchService: MultiDraftSearchService = new MultiDraftSearchService {
      override val enableExplanations: Boolean = true
    }

    val Success(search1) =
      searchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByParentTopicNameAsc)): @unchecked
    search1.summaryResults.map(_.id) should be(List(1, 2, 3))

    val Success(search2) =
      searchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByPrimaryRootAsc)): @unchecked
    search2.summaryResults.map(_.id) should be(List(2, 3, 1))

    val Success(search3) =
      searchService.matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByResourceTypeAsc)): @unchecked
    search3.summaryResults.map(_.id) should be(List(3, 1, 2))

  }

  test("Test that filtering published dates works as expected") {
    val today = NDLADate.now().withNano(0)

    val draft1 = TestData.draft1.copy(id = Some(1), published = today.plusDays(1))
    val draft2 = TestData.draft1.copy(id = Some(2), published = today.minusDays(10))
    val draft3 = TestData.draft1.copy(id = Some(3), published = today.minusDays(10))
    val draft4 = TestData.draft1.copy(id = Some(4), published = today.minusDays(15))
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(publishedFilterFrom = Some(today), publishedFilterTo = None))
      .get
      .summaryResults
      .map(_.id) should be(Seq(1))

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(publishedFilterFrom = None, publishedFilterTo = Some(today)))
      .get
      .summaryResults
      .map(_.id) should be(Seq(2, 3, 4))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          publishedFilterFrom = Some(today.minusDays(11)),
          publishedFilterTo = Some(today.plusDays(2)),
        )
      )
      .get
      .summaryResults
      .map(_.id) should be(Seq(1, 2, 3))
  }

  test("Test sorting published dates works as expected") {
    val today = NDLADate.now().withNano(0)

    val draft1 = TestData.draft1.copy(id = Some(1), published = today.plusDays(1))
    val draft2 = TestData.draft1.copy(id = Some(2), published = today.minusDays(12))
    val draft3 = TestData.draft1.copy(id = Some(3), published = today.minusDays(10))
    val draft4 = TestData.draft1.copy(id = Some(4), published = today.minusDays(15))
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByPublishedAsc))
      .get
      .summaryResults
      .map(_.id) should be(Seq(4, 2, 3, 1))

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByPublishedDesc))
      .get
      .summaryResults
      .map(_.id) should be(Seq(1, 3, 2, 4))
  }

  test("Test that concepts appear in the search, but not by default") {
    val draft1 = TestData.draft1.copy(id = Some(1))
    val draft2 = TestData.draft1.copy(id = Some(2))
    val draft3 = TestData.draft1.copy(id = Some(3))
    val draft4 = TestData.draft1.copy(id = Some(4))

    val concept1 = TestData.sampleNbDomainConcept.copy(id = Some(1))
    val concept2 = TestData.sampleNbDomainConcept.copy(id = Some(2))
    val concept3 = TestData.sampleNbDomainConcept.copy(id = Some(3))
    val concept4 = TestData.sampleNbDomainConcept.copy(id = Some(4))
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get
    draftConceptIndexService.indexDocument(concept1, indexingBundle).get
    draftConceptIndexService.indexDocument(concept2, indexingBundle).get
    draftConceptIndexService.indexDocument(concept3, indexingBundle).get
    draftConceptIndexService.indexDocument(concept4, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4 && draftConceptIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(multiDraftSearchSettings.copy(sort = Sort.ByIdAsc))
      .get
      .summaryResults
      .map(r => r.id -> r.resultType) should be(
      Seq(1 -> SearchType.Drafts, 2 -> SearchType.Drafts, 3 -> SearchType.Drafts, 4 -> SearchType.Drafts)
    )

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          sort = Sort.ByIdAsc,
          resultTypes = Some(List(SearchType.Drafts, SearchType.Concepts)),
        )
      )
      .get
      .summaryResults
      .map(r => r.id -> r.resultType) should be(
      Seq(
        1 -> SearchType.Concepts,
        1 -> SearchType.Drafts,
        2 -> SearchType.Concepts,
        2 -> SearchType.Drafts,
        3 -> SearchType.Concepts,
        3 -> SearchType.Drafts,
        4 -> SearchType.Concepts,
        4 -> SearchType.Drafts,
      )
    )
  }
  test("that concepts are indexed with content and are searchable") {
    val draft1 = TestData.draft1.copy(id = Some(1))
    val draft2 = TestData.draft1.copy(id = Some(2))
    val draft3 = TestData.draft1.copy(id = Some(3))

    val concept1 = TestData
      .sampleNbDomainConcept
      .copy(id = Some(1), content = Seq(ConceptContent("Liten apekatt", "nb")))
    val concept2 = TestData.sampleNbDomainConcept.copy(id = Some(2), content = Seq(ConceptContent("Stor giraff", "nb")))
    val concept3 = TestData
      .sampleNbDomainConcept
      .copy(id = Some(3), content = Seq(ConceptContent("Medium kylling", "nb")))
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftConceptIndexService.indexDocument(concept1, indexingBundle).get
    draftConceptIndexService.indexDocument(concept2, indexingBundle).get
    draftConceptIndexService.indexDocument(concept3, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 3 && draftConceptIndexService.countDocuments == 3)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          sort = Sort.ByIdAsc,
          query = NonEmptyString.fromString("giraff"),
          resultTypes = Some(List(SearchType.Drafts, SearchType.Concepts, SearchType.LearningPaths)),
        )
      )
      .get
      .summaryResults
      .map(r => r.id -> r.resultType) should be(Seq(2 -> SearchType.Concepts))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          sort = Sort.ByIdAsc,
          query = NonEmptyString.fromString("apekatt"),
          resultTypes = Some(List(SearchType.Drafts, SearchType.Concepts, SearchType.LearningPaths)),
        )
      )
      .get
      .summaryResults
      .map(r => r.id -> r.resultType) should be(Seq(1 -> SearchType.Concepts))
  }

  test("That filtering based on learningResourceType works for everyone") {
    val draft1        = TestData.draft1.copy(id = Some(1), articleType = ArticleType.Standard)
    val draft2        = TestData.draft1.copy(id = Some(2), articleType = ArticleType.TopicArticle)
    val learningPath3 = TestData.learningPath1.copy(id = Some(3))
    val concept4      = TestData.sampleNbDomainConcept.copy(id = Some(4), conceptType = ConceptType.CONCEPT)
    val concept5      = TestData.sampleNbDomainConcept.copy(id = Some(5), conceptType = ConceptType.GLOSS)

    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    learningPathIndexService.indexDocument(learningPath3, indexingBundle).get
    draftConceptIndexService.indexDocument(concept4, indexingBundle).get
    draftConceptIndexService.indexDocument(concept5, indexingBundle).get

    blockUntil(() =>
      draftIndexService.countDocuments == 2 &&
        learningPathIndexService.countDocuments == 1 &&
        draftConceptIndexService.countDocuments == 2
    )

    {
      val search = multiDraftSearchService
        .matchingQuery(
          multiDraftSearchSettings.copy(
            learningResourceTypes = List(LearningResourceType.Article),
            resultTypes = Some(List(SearchType.Drafts, SearchType.Concepts, SearchType.LearningPaths)),
          )
        )
        .get
      search.summaryResults.map(_.id) should be(Seq(1))
    }
    {
      val search = multiDraftSearchService
        .matchingQuery(
          multiDraftSearchSettings.copy(
            learningResourceTypes = List(LearningResourceType.TopicArticle),
            resultTypes = Some(List(SearchType.Drafts, SearchType.Concepts, SearchType.LearningPaths)),
          )
        )
        .get
      search.summaryResults.map(_.id) should be(Seq(2))
    }
    {
      val search = multiDraftSearchService
        .matchingQuery(
          multiDraftSearchSettings.copy(
            learningResourceTypes = List(LearningResourceType.LearningPath),
            resultTypes = Some(List(SearchType.Drafts, SearchType.Concepts, SearchType.LearningPaths)),
          )
        )
        .get
      search.summaryResults.map(_.id) should be(Seq(3))
    }
    {
      val search = multiDraftSearchService
        .matchingQuery(
          multiDraftSearchSettings.copy(
            learningResourceTypes = List(LearningResourceType.Concept),
            resultTypes = Some(List(SearchType.Drafts, SearchType.Concepts, SearchType.LearningPaths)),
          )
        )
        .get
      search.summaryResults.map(_.id) should be(Seq(4))
    }
    {
      val search = multiDraftSearchService
        .matchingQuery(
          multiDraftSearchSettings.copy(
            learningResourceTypes = List(LearningResourceType.Gloss),
            resultTypes = Some(List(SearchType.Drafts, SearchType.Concepts, SearchType.LearningPaths)),
          )
        )
        .get
      search.summaryResults.map(_.id) should be(Seq(5))
    }
  }

  test("That responsible filtering works for concepts") {
    val responsible = Responsible("some-user", TestData.today)
    val draft1      = TestData.draft1.copy(id = Some(1), articleType = ArticleType.Standard, responsible = Some(responsible))
    val draft2      = TestData.draft1.copy(id = Some(2), articleType = ArticleType.Standard, responsible = None)
    val concept3    = TestData
      .sampleNbDomainConcept
      .copy(id = Some(3), responsible = Some(responsible), conceptType = ConceptType.CONCEPT)

    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftConceptIndexService.indexDocument(concept3, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 2 && draftConceptIndexService.countDocuments == 1)

    val search = multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          resultTypes = Some(List(SearchType.Drafts, SearchType.Concepts, SearchType.LearningPaths)),
          responsibleIdFilter = List("some-user"),
        )
      )
      .get
    search.summaryResults.map(_.id) should be(Seq(1, 3))
  }

}
