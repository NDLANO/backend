/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.common.model.domain.draft.{DraftResponsible, DraftStatus, RevisionMeta, RevisionStatus}
import no.ndla.common.model.domain.{ArticleContent, EditorNote, Status, Title}
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.Elastic4sClientFactory
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.model.api.ApiTaxonomyContext
import no.ndla.searchapi.model.domain.Sort
import no.ndla.searchapi.model.taxonomy._
import no.ndla.searchapi.{TestData, TestEnvironment}
import org.scalatest.Outcome

import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success}

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

  test("That search on embed id supports embed with multiple resources") {
    val draft1 = TestData.draft1.copy(
      id = Some(1),
      content = Seq(
        ArticleContent(
          """<section><div data-type="related-content"><embed data-article-id="3" data-resource="related-content"></div></section>""",
          "nb"
        )
      )
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      content = Seq(
        ArticleContent(
          """<section><embed data-content-id="3" data-link-text="Test?" data-resource="content-link"></section>""",
          "nb"
        )
      )
    )
    val draft3 = TestData.draft1.copy(id = Some(3))
    draftIndexService.indexDocument(draft1, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft2, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft3, taxonomyTestBundle, Some(grepBundle)).get

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
    val today     = LocalDateTime.now().withNano(0)
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
    draftIndexService.indexDocument(draft1, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft2, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft3, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft4, taxonomyTestBundle, Some(grepBundle)).get

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
    val today     = LocalDateTime.now().withNano(0)
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
    draftIndexService.indexDocument(draft1, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft2, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft3, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft4, taxonomyTestBundle, Some(grepBundle)).get

    blockUntil(() => draftIndexService.countDocuments == 4)

    val Success(search1) =
      multiDraftSearchService.matchingQuery(
        multiDraftSearchSettings.copy(query = Some("trylleformel"))
      )

    search1.totalCount should be(1)
    search1.results.map(_.id) should be(List(3))
  }

  test("Test that filtering revision dates works as expected") {
    val today = LocalDateTime.now().withNano(0)

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
    draftIndexService.indexDocument(draft1, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft2, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft3, taxonomyTestBundle, Some(grepBundle)).get
    draftIndexService.indexDocument(draft4, taxonomyTestBundle, Some(grepBundle)).get

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
    val today = LocalDateTime.now().withNano(0)

    val status = Status(current = DraftStatus.DRAFT, other = Set.empty)
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
    draftIndexService.indexDocument(draft1, taxonomyTestBundle, Some(grepBundle)).failIfFailure
    draftIndexService.indexDocument(draft2, taxonomyTestBundle, Some(grepBundle)).failIfFailure
    draftIndexService.indexDocument(draft3, taxonomyTestBundle, Some(grepBundle)).failIfFailure

    blockUntil(() => draftIndexService.countDocuments == 3)

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          query = Some("Gris"),
          excludeRevisionHistory = true
        )
      )
      .get
      .results
      .map(_.id) should be(Seq(2, 3))

    multiDraftSearchService
      .matchingQuery(
        multiDraftSearchSettings.copy(
          query = Some("Gris"),
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
      responsible = Some(DraftResponsible("hei", TestData.today))
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      responsible = Some(DraftResponsible("hei2", TestData.today))
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      responsible = Some(DraftResponsible("hei", TestData.today))
    )
    draftIndexService.indexDocument(draft1, taxonomyTestBundle, Some(grepBundle)).failIfFailure
    draftIndexService.indexDocument(draft2, taxonomyTestBundle, Some(grepBundle)).failIfFailure
    draftIndexService.indexDocument(draft3, taxonomyTestBundle, Some(grepBundle)).failIfFailure

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
      responsible = Some(DraftResponsible("hei", TestData.today.minusDays(5)))
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      responsible = Some(DraftResponsible("hei2", TestData.today.minusDays(2)))
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      responsible = Some(DraftResponsible("hei", TestData.today.minusDays(3)))
    )
    val draft4 = TestData.draft1.copy(
      id = Some(4),
      responsible = None
    )
    draftIndexService.indexDocument(draft1, taxonomyTestBundle, Some(grepBundle)).failIfFailure
    draftIndexService.indexDocument(draft2, taxonomyTestBundle, Some(grepBundle)).failIfFailure
    draftIndexService.indexDocument(draft3, taxonomyTestBundle, Some(grepBundle)).failIfFailure
    draftIndexService.indexDocument(draft4, taxonomyTestBundle, Some(grepBundle)).failIfFailure

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

    val taxonomyBundle = {
      TaxonomyBundle(
        relevances,
        List.empty,
        resourceTypes,
        subjects = List(
          TaxSubject(
            "urn:subject:1",
            "Matte",
            None,
            Some("/subject:1"),
            visibleMetadata,
            List.empty
          )
        ),
        topics = List(
          Topic(
            "urn:topic:1",
            "T1",
            Some(s"urn:article:${draft1.id.get}"),
            Some("/subject:1/topic:1"),
            visibleMetadata,
            List.empty
          ),
          Topic(
            "urn:topic:2",
            "T2",
            Some(s"urn:article:${draft2.id.get}"),
            Some("/subject:1/topic:2"),
            visibleMetadata,
            List.empty
          ),
          Topic(
            "urn:topic:3",
            "T3",
            Some(s"urn:article:${draft3.id.get}"),
            Some("/subject:1/topic:1/topic:3"),
            visibleMetadata,
            List.empty
          ),
          Topic(
            "urn:topic:4",
            "T4",
            Some(s"urn:article:${draft4.id.get}"),
            Some("/subject:1/topic:1/topic:4"),
            visibleMetadata,
            List.empty
          )
        ),
        resources = List(
          Resource(
            "urn:resource:5",
            "R5",
            Some(s"urn:article:${draft5.id.get}"),
            Some("/subject:1/topic:1/resource:5"),
            visibleMetadata,
            List.empty
          ),
          Resource(
            "urn:resource:6",
            "R6",
            Some(s"urn:article:${draft5.id.get}"),
            Some("/subject:1/topic:1/topic:3/resource:6"),
            visibleMetadata,
            List.empty
          )
        ),
        subjectTopicConnections = List(
          SubjectTopicConnection(
            "urn:subject:1",
            "urn:topic:1",
            "urn:subject-topic:1",
            primary = true,
            1,
            Some("urn:relevance:core")
          ),
          SubjectTopicConnection(
            "urn:subject:1",
            "urn:topic:2",
            "urn:subject-topic:2",
            primary = true,
            1,
            Some("urn:relevance:core")
          )
        ),
        topicSubtopicConnections = List(
          TopicSubtopicConnection(
            "urn:topic:1",
            "urn:topic:3",
            "urn:topic-subtopic:1",
            primary = true,
            1,
            Some("urn:relevance:core")
          ),
          TopicSubtopicConnection(
            "urn:topic:1",
            "urn:topic:4",
            "urn:topic-subtopic:2",
            primary = true,
            1,
            Some("urn:relevance:core")
          )
        ),
        topicResourceConnections = List(
          TopicResourceConnection(
            "urn:topic:1",
            "urn:resource:5",
            "urn:topic-resource:1",
            primary = true,
            1,
            Some("urn:relevance:core")
          ),
          TopicResourceConnection(
            "urn:topic:3",
            "urn:resource:5",
            "urn:topic-resource:1",
            primary = false,
            1,
            Some("urn:relevance:core")
          ),
          TopicResourceConnection(
            "urn:topic:3",
            "urn:resource:6",
            "urn:topic-resource:2",
            primary = true,
            1,
            Some("urn:relevance:core")
          )
        )
      )
    }

    {
      draftIndexService.indexDocument(draft1, taxonomyBundle, Some(grepBundle)).failIfFailure
      draftIndexService.indexDocument(draft2, taxonomyBundle, Some(grepBundle)).failIfFailure
      draftIndexService.indexDocument(draft3, taxonomyBundle, Some(grepBundle)).failIfFailure
      draftIndexService.indexDocument(draft4, taxonomyBundle, Some(grepBundle)).failIfFailure
      draftIndexService.indexDocument(draft5, taxonomyBundle, Some(grepBundle)).failIfFailure
      blockUntil(() => draftIndexService.countDocuments == 5)
    }

    val result = multiDraftSearchService.matchingQuery(multiDraftSearchSettings).get

    def ctxsFor(id: Long): List[ApiTaxonomyContext] = result.results.find(_.id == id).get.contexts
    def ctxFor(id: Long): ApiTaxonomyContext = {
      val ctxs = ctxsFor(id)
      ctxs.length should be(1)
      ctxs.head
    }

    ctxFor(1).isPrimaryConnection should be(true)
    ctxFor(2).isPrimaryConnection should be(true)
    ctxFor(3).isPrimaryConnection should be(true)
    ctxFor(4).isPrimaryConnection should be(true)
    ctxsFor(5).map(_.isPrimaryConnection) should be(Seq(true, false, true))

  }
}
