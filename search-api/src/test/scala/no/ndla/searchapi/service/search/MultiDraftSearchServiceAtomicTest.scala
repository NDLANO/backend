/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticApi.indexInto
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import no.ndla.common.CirceUtil
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.MyNDLABundle
import no.ndla.common.model.domain.*
import no.ndla.common.model.domain.draft.{Draft, DraftStatus, RevisionMeta, RevisionStatus}
import no.ndla.common.model.domain.{EditorNote, Priority, Responsible}
import no.ndla.network.tapir.NonEmptyString
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.TestData.*
import no.ndla.searchapi.model.api.ApiTaxonomyContext
import no.ndla.searchapi.model.domain.{IndexingBundle, Sort}
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.taxonomy.*
import no.ndla.searchapi.{TestData, TestEnvironment}
import org.scalatest.Outcome

import java.util.UUID
import scala.util.{Failure, Success, Try}

class MultiDraftSearchServiceAtomicTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
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

  override val articleIndexService: ArticleIndexService = new ArticleIndexService {
    override val indexShards = 1
  }
  override val draftIndexService: DraftIndexService = new DraftIndexService {
    override val indexShards = 1
  }
  override val learningPathIndexService: LearningPathIndexService = new LearningPathIndexService {
    override val indexShards = 1
  }
  override val multiDraftSearchService = new MultiDraftSearchService
  override val converterService        = new ConverterService
  override val searchConverterService  = new SearchConverterService

  override def beforeEach(): Unit = {
    if (elasticSearchContainer.isSuccess) {
      articleIndexService.createIndexAndAlias()
      draftIndexService.createIndexAndAlias()
      learningPathIndexService.createIndexAndAlias()
    }
  }

  override def afterEach(): Unit = {
    if (elasticSearchContainer.isSuccess) {
      articleIndexService.deleteIndexAndAlias()
      draftIndexService.deleteIndexAndAlias()
      learningPathIndexService.deleteIndexAndAlias()
    }
  }

  val indexingBundle: IndexingBundle = IndexingBundle(
    grepBundle = Some(grepBundle),
    taxonomyBundle = Some(taxonomyTestBundle),
    myndlaBundle = Some(TestData.myndlaTestBundle)
  )

  test("That search on embed id supports embed with multiple resources") {
    val draft1 = TestData.draft1.copy(
      id = Some(1),
      content = Seq(
        ArticleContent(
          s"""<section><div data-type="related-content"><$EmbedTagName data-article-id="3" data-resource="related-content"></div></section>""",
          "nb"
        )
      )
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      content = Seq(
        ArticleContent(
          s"""<section><$EmbedTagName data-content-id="3" data-resource="content-link">Test?</$EmbedTagName></section>""",
          "nb"
        )
      )
    )
    val draft3 = TestData.draft1.copy(id = Some(3))
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 3)

    val Success(search1) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(embedId = Some("3"), embedResource = List("content-link"))
      )

    search1.totalCount should be(1)
    search1.results.map(_.id) should be(List(2))

    val Success(search2) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(embedId = Some("3"), embedResource = List("content-link", "related-content"))
      )

    search2.totalCount should be(2)
    search2.results.map(_.id) should be(List(1, 2))
  }

  test("That sorting by revision date sorts by the earliest 'needs-revision'") {
    val today     = NDLADate.now().withNano(0)
    val yesterday = today.minusDays(1)
    val tomorrow  = today.plusDays(1)

    val draft1 = TestData.draft1.copy(
      id = Some(1),
      revisionMeta = Seq(
        RevisionMeta(
          id = UUID.randomUUID(),
          today,
          note = "note",
          status = RevisionStatus.NeedsRevision
        ),
        RevisionMeta(
          id = UUID.randomUUID(),
          tomorrow,
          note = "note",
          status = RevisionStatus.NeedsRevision
        ),
        RevisionMeta(
          id = UUID.randomUUID(),
          yesterday,
          note = "note",
          status = RevisionStatus.Revised
        )
      )
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      revisionMeta = Seq(
        RevisionMeta(
          id = UUID.randomUUID(),
          yesterday.minusDays(10),
          note = "note",
          status = RevisionStatus.Revised
        )
      )
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      revisionMeta = Seq(
        RevisionMeta(
          id = UUID.randomUUID(),
          yesterday,
          note = "note",
          status = RevisionStatus.NeedsRevision
        )
      )
    )
    val draft4 = TestData.draft1.copy(
      id = Some(4),
      revisionMeta = Seq()
    )
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4)

    val Success(search1) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(sort = Sort.ByRevisionDateAsc)
      )

    search1.totalCount should be(4)
    search1.results.map(_.id) should be(List(3, 1, 2, 4))

    val Success(search2) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(sort = Sort.ByRevisionDateDesc)
      )

    search2.totalCount should be(4)
    search2.results.map(_.id) should be(List(1, 3, 2, 4))
  }

  test("Test that searching for note in revision meta works as expected") {
    val today     = NDLADate.now().withNano(0)
    val yesterday = today.minusDays(1)
    val tomorrow  = today.plusDays(1)

    val draft1 = TestData.draft1.copy(
      id = Some(1),
      revisionMeta = Seq(
        RevisionMeta(
          id = UUID.randomUUID(),
          today,
          note = "apekatt",
          status = RevisionStatus.NeedsRevision
        ),
        RevisionMeta(
          id = UUID.randomUUID(),
          tomorrow,
          note = "note",
          status = RevisionStatus.NeedsRevision
        ),
        RevisionMeta(
          id = UUID.randomUUID(),
          yesterday,
          note = "note",
          status = RevisionStatus.Revised
        )
      )
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      revisionMeta = Seq(
        RevisionMeta(
          id = UUID.randomUUID(),
          yesterday.minusDays(10),
          note = "kinakål",
          status = RevisionStatus.Revised
        )
      )
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      revisionMeta = Seq(
        RevisionMeta(
          id = UUID.randomUUID(),
          yesterday,
          note = "trylleformel",
          status = RevisionStatus.NeedsRevision
        )
      )
    )
    val draft4 = TestData.draft1.copy(
      id = Some(4),
      revisionMeta = Seq()
    )
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4)

    val Success(search1) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some(NonEmptyString.fromString("trylleformel").get))
      )

    search1.totalCount should be(1)
    search1.results.map(_.id) should be(List(3))
  }

  test("Test that filtering revision dates works as expected") {
    val today = NDLADate.now().withNano(0)

    val draft1 = TestData.draft1.copy(
      id = Some(1),
      revisionMeta = Seq(
        RevisionMeta(
          id = UUID.randomUUID(),
          today.plusDays(1),
          note = "apekatt",
          status = RevisionStatus.NeedsRevision
        ),
        RevisionMeta(
          id = UUID.randomUUID(),
          today.plusDays(10),
          note = "note",
          status = RevisionStatus.Revised
        )
      )
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      revisionMeta = Seq(
        RevisionMeta(
          id = UUID.randomUUID(),
          today.minusDays(10),
          note = "kinakål",
          status = RevisionStatus.Revised
        )
      )
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      revisionMeta = Seq(
        RevisionMeta(
          id = UUID.randomUUID(),
          today.minusDays(10),
          note = "trylleformel",
          status = RevisionStatus.NeedsRevision
        )
      )
    )
    val draft4 = TestData.draft1.copy(
      id = Some(4),
      revisionMeta = Seq()
    )
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          revisionDateFilterFrom = Some(today),
          revisionDateFilterTo = None
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          revisionDateFilterFrom = None,
          revisionDateFilterTo = Some(today)
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(3))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          revisionDateFilterFrom = Some(today.minusDays(11)),
          revisionDateFilterTo = Some(today.plusDays(1))
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1, 3))
  }

  test("That hits from revision log is not included when exclude param is set") {
    val today = NDLADate.now().withNano(0)

    val status = Status(current = DraftStatus.PLANNED, other = Set.empty)
    val mkNote = (n: String) => EditorNote(n, "some-user", status, today)

    val draft1 = TestData.draft1.copy(
      id = Some(1),
      notes = Seq(
        mkNote("Katt"),
        mkNote("Hund")
      ),
      previousVersionsNotes = Seq(
        mkNote("Tiger"),
        mkNote("Gris")
      )
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      notes = Seq(
        mkNote("Kinakål"),
        mkNote("Grevling"),
        mkNote("Apekatt"),
        mkNote("Gris")
      ),
      previousVersionsNotes = Seq(
        mkNote("Giraff")
      )
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      title = Seq(Title("Gris", "nb")),
      notes = Seq(),
      previousVersionsNotes = Seq()
    )
    draftIndexService.indexDocument(draft1, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft2, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft3, indexingBundle).failIfFailure

    blockUntil(() => draftIndexService.countDocuments == 3)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          query = Some(NonEmptyString.fromString("Gris").get),
          excludeRevisionHistory = true
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(2, 3))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          query = Some(NonEmptyString.fromString("Gris").get),
          excludeRevisionHistory = false
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1, 2, 3))
  }

  test("That responsibleId is filterable") {
    val draft1 = TestData.draft1.copy(
      id = Some(1),
      responsible = Some(Responsible("hei", TestData.today))
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      responsible = Some(Responsible("hei2", TestData.today))
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      responsible = Some(Responsible("hei", TestData.today))
    )
    draftIndexService.indexDocument(draft1, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft2, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft3, indexingBundle).failIfFailure

    blockUntil(() => draftIndexService.countDocuments == 3)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          responsibleIdFilter = List.empty
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1, 2, 3))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          responsibleIdFilter = List("hei")
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1, 3))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          responsibleIdFilter = List("hei2")
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(2))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          responsibleIdFilter = List("hei", "hei2")
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1, 2, 3))
  }

  test("That responsible lastUpdated is sortable") {
    val draft1 = TestData.draft1.copy(
      id = Some(1),
      responsible = Some(Responsible("hei", TestData.today.minusDays(5)))
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      responsible = Some(Responsible("hei2", TestData.today.minusDays(2)))
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      responsible = Some(Responsible("hei", TestData.today.minusDays(3)))
    )
    val draft4 = TestData.draft1.copy(
      id = Some(4),
      responsible = None
    )
    draftIndexService.indexDocument(draft1, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft2, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft3, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft4, indexingBundle).failIfFailure

    blockUntil(() => draftIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          responsibleIdFilter = List.empty,
          sort = Sort.ByResponsibleLastUpdatedAsc
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1, 3, 2, 4))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          responsibleIdFilter = List.empty,
          sort = Sort.ByResponsibleLastUpdatedDesc
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(2, 3, 1, 4))
  }

  test("That primary connections are handled correctly") {
    val draft1 = TestData.draft1.copy(id = Some(1)) // T1
    val draft2 = TestData.draft1.copy(id = Some(2)) // T2
    val draft3 = TestData.draft1.copy(id = Some(3)) // T3
    val draft4 = TestData.draft1.copy(id = Some(4)) // T4
    val draft5 = TestData.draft1.copy(id = Some(5)) // R5 + R6

    val subject_1 = Node(
      "urn:subject:1",
      "Matte",
      None,
      Some("/subject:1"),
      visibleMetadata,
      List.empty,
      NodeType.SUBJECT,
      List(
        TaxonomyContext(
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
          isActive = true
        )
      )
    )
    val topic_1 = Node(
      "urn:topic:1",
      "T1",
      Some(s"urn:article:${draft1.id.get}"),
      Some("/subject:1/topic:1"),
      visibleMetadata,
      List.empty,
      NodeType.TOPIC,
      List.empty
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
      isActive = true
    )
    val topic_2 = Node(
      "urn:topic:2",
      "T2",
      Some(s"urn:article:${draft2.id.get}"),
      Some("/subject:1/topic:2"),
      visibleMetadata,
      List.empty,
      NodeType.TOPIC,
      List.empty
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
      isActive = true
    )
    val topic_3 = Node(
      "urn:topic:3",
      "T3",
      Some(s"urn:article:${draft3.id.get}"),
      Some("/subject:1/topic:1/topic:3"),
      visibleMetadata,
      List.empty,
      NodeType.TOPIC,
      List.empty
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
      isActive = true
    )
    val topic_4 = Node(
      "urn:topic:4",
      "T4",
      Some(s"urn:article:${draft4.id.get}"),
      Some("/subject:1/topic:1/topic:4"),
      visibleMetadata,
      List.empty,
      NodeType.TOPIC,
      List.empty
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
      isActive = true
    )
    val resource_5 = Node(
      "urn:resource:5",
      "R5",
      Some(s"urn:article:${draft5.id.get}"),
      Some("/subject:1/topic:1/resource:5"),
      visibleMetadata,
      List.empty,
      NodeType.RESOURCE,
      List.empty
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
      isActive = true
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
        isActive = true
      )
    val resource_6 = Node(
      "urn:resource:6",
      "R6",
      Some(s"urn:article:${draft5.id.get}"),
      Some("/subject:1/topic:1/topic:3/resource:6"),
      visibleMetadata,
      List.empty,
      NodeType.RESOURCE,
      List.empty
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
      isActive = true
    )

    val taxonomyBundle = {
      TaxonomyBundle(nodes =
        List(
          subject_1,
          topic_1,
          topic_2,
          topic_3,
          topic_4,
          resource_5,
          resource_6
        )
      )
    }

    {
      draftIndexService.indexDocument(draft1, indexingBundle).failIfFailure
      draftIndexService.indexDocument(draft2, indexingBundle).failIfFailure
      draftIndexService.indexDocument(draft3, indexingBundle).failIfFailure
      draftIndexService.indexDocument(draft4, indexingBundle).failIfFailure
      draftIndexService.indexDocument(draft5, indexingBundle).failIfFailure
      blockUntil(() => draftIndexService.countDocuments == 5)
    }

    val result = multiDraftSearchService.matchingQuery(multiDraftSearchSettings).get

    def ctxsFor(id: Long): List[ApiTaxonomyContext] = result.results.find(_.id == id).get.contexts
    def ctxFor(id: Long): ApiTaxonomyContext = {
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
    val draft1 = TestData.draft1.copy(
      id = Some(1),
      status = Status(DraftStatus.PLANNED, Set.empty)
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      status = Status(DraftStatus.PUBLISHED, Set.empty)
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      status = Status(DraftStatus.LANGUAGE, Set.empty)
    )
    val draft4 = TestData.draft1.copy(
      id = Some(4),
      status = Status(DraftStatus.FOR_APPROVAL, Set.empty)
    )

    draftIndexService.indexDocument(draft1, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft2, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft3, indexingBundle).failIfFailure
    draftIndexService.indexDocument(draft4, indexingBundle).failIfFailure

    blockUntil(() => draftIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          responsibleIdFilter = List.empty,
          sort = Sort.ByStatusAsc
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(4, 3, 1, 2))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          responsibleIdFilter = List.empty,
          sort = Sort.ByStatusDesc
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(2, 1, 3, 4))
  }

  test("Test that filtering prioritized works as expected") {

    val draft1 = TestData.draft1.copy(
      id = Some(1),
      priority = Priority.Unspecified
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      priority = Priority.Prioritized
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      priority = Priority.Prioritized
    )
    val draft4 = TestData.draft1.copy(
      id = Some(4)
    )
    val draft5 = TestData.draft1.copy(
      id = Some(5),
      priority = Priority.OnHold
    )
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get
    draftIndexService.indexDocument(draft5, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 5)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          priority = List(Priority.Prioritized.entryName)
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(2, 3))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          priority = List(Priority.Unspecified.entryName)
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1, 4))
    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          priority = List(Priority.OnHold.entryName)
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(5))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          priority = List.empty
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1, 2, 3, 4, 5))
  }

  test("That search on embed id supports video embed with timestamp resources") {
    val videoId = "66772123"
    val draft1 = TestData.draft1.copy(
      id = Some(1),
      content = Seq(
        ArticleContent(
          s"""<section><div data-type="related-content"><$EmbedTagName data-resource="video" data-videoid="$videoId&amp;t=1"></div></section>""",
          "nb"
        )
      )
    )
    draftIndexService.indexDocument(draft1, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 1)

    val Success(search1) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(embedId = Some(videoId), embedResource = List("video"))
      )

    search1.totalCount should be(1)
    search1.results.map(_.id) should be(List(1))
  }

  test("That sorting on resource types works as expected") {
    val indexService = new DraftIndexService {
      override val indexShards = 1
      override def createIndexRequest(
          domainModel: Draft,
          indexName: String,
          indexingBundle: IndexingBundle
      ): Try[IndexRequest] = {

        val draft = domainModel.id.get match {
          case 1 =>
            TestData.searchableDraft.copy(
              id = 1,
              parentTopicName = SearchableLanguageValues.from("nb" -> "Apekatt emne"),
              defaultParentTopicName = Some("Apekatt emne"),
              primaryRoot = SearchableLanguageValues.from("nb" -> "Capekatt rot"),
              defaultRoot = Some("Capekatt rot"),
              resourceTypeName = SearchableLanguageValues.from("nb" -> "Bapekatt ressurs"),
              defaultResourceTypeName = Some("Bapekatt ressurs")
            )
          case 2 =>
            TestData.searchableDraft.copy(
              id = 2,
              parentTopicName = SearchableLanguageValues.from("nb" -> "Bpekatt emne"),
              defaultParentTopicName = Some("Bpekatt emne"),
              primaryRoot = SearchableLanguageValues.from("nb" -> "Apekatt rot"),
              defaultRoot = Some("Apekatt rot"),
              resourceTypeName = SearchableLanguageValues.from("nb" -> "Capekatt ressurs"),
              defaultResourceTypeName = Some("Capekatt ressurs")
            )
          case 3 =>
            TestData.searchableDraft.copy(
              id = 3,
              parentTopicName = SearchableLanguageValues.from("nb" -> "Cpekatt emne"),
              defaultParentTopicName = Some("Cpekatt emne"),
              primaryRoot = SearchableLanguageValues.from("nb" -> "Bapekatt rot"),
              defaultRoot = Some("Bapekatt rot"),
              resourceTypeName = SearchableLanguageValues.from("nb" -> "Apekatt ressurs"),
              defaultResourceTypeName = Some("Apekatt ressurs")
            )
          case _ => fail("Unexpected id, this is a bug with the test")
        }

        val source = CirceUtil.toJsonString(draft)
        Success(indexInto(indexName).doc(source).id(domainModel.id.get.toString))
      }
    }

    indexService.indexDocument(TestData.draft1.copy(id = Some(1)), indexingBundle).get
    indexService.indexDocument(TestData.draft1.copy(id = Some(2)), indexingBundle).get
    indexService.indexDocument(TestData.draft1.copy(id = Some(3)), indexingBundle).get

    blockUntil(() => indexService.countDocuments == 3)
    val searchService = new MultiDraftSearchService

    val Success(search1) = searchService.matchingQuery(
      multiDraftSearchSettings.copy(sort = Sort.ByParentTopicNameAsc)
    )
    search1.results.map(_.id) should be(List(1, 2, 3))

    val Success(search2) = searchService.matchingQuery(
      multiDraftSearchSettings.copy(sort = Sort.ByPrimaryRootAsc)
    )
    search2.results.map(_.id) should be(List(2, 3, 1))

    val Success(search3) = searchService.matchingQuery(
      multiDraftSearchSettings.copy(sort = Sort.ByResourceTypeAsc)
    )
    search3.results.map(_.id) should be(List(3, 1, 2))

  }

  test("Test that filtering published dates works as expected") {
    val today = NDLADate.now().withNano(0)

    val draft1 = TestData.draft1.copy(
      id = Some(1),
      published = today.plusDays(1)
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      published = today.minusDays(10)
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      published = today.minusDays(10)
    )
    val draft4 = TestData.draft1.copy(
      id = Some(4),
      published = today.minusDays(15)
    )
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          publishedFilterFrom = Some(today),
          publishedFilterTo = None
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          publishedFilterFrom = None,
          publishedFilterTo = Some(today)
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(2, 3, 4))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          publishedFilterFrom = Some(today.minusDays(11)),
          publishedFilterTo = Some(today.plusDays(2))
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1, 2, 3))
  }

  test("Test sorting published dates works as expected") {
    val today = NDLADate.now().withNano(0)

    val draft1 = TestData.draft1.copy(
      id = Some(1),
      published = today.plusDays(1)
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      published = today.minusDays(12)
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      published = today.minusDays(10)
    )
    val draft4 = TestData.draft1.copy(
      id = Some(4),
      published = today.minusDays(15)
    )
    draftIndexService.indexDocument(draft1, indexingBundle).get
    draftIndexService.indexDocument(draft2, indexingBundle).get
    draftIndexService.indexDocument(draft3, indexingBundle).get
    draftIndexService.indexDocument(draft4, indexingBundle).get

    blockUntil(() => draftIndexService.countDocuments == 4)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          sort = Sort.ByPublishedAsc
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(4, 2, 3, 1))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          sort = Sort.ByPublishedDesc
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(1, 3, 2, 4))
  }

}
