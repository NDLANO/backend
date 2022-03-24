/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.Elastic4sClientFactory
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.model.domain.Sort
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.domain.draft.RevisionMeta
import no.ndla.searchapi.{TestData, TestEnvironment}
import org.scalatest.Outcome

import java.time.LocalDateTime
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
          today,
          note = "note",
          status = "needs-revision"
        ),
        RevisionMeta(
          tomorrow,
          note = "note",
          status = "needs-revision"
        ),
        RevisionMeta(
          yesterday,
          note = "note",
          status = "revised"
        )
      )
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      revisionMeta = Seq(
        RevisionMeta(
          yesterday.minusDays(10),
          note = "note",
          status = "revised"
        )
      )
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      revisionMeta = Seq(
        RevisionMeta(
          yesterday,
          note = "note",
          status = "needs-revision"
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
          today,
          note = "apekatt",
          status = "needs-revision"
        ),
        RevisionMeta(
          tomorrow,
          note = "note",
          status = "needs-revision"
        ),
        RevisionMeta(
          yesterday,
          note = "note",
          status = "revised"
        )
      )
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      revisionMeta = Seq(
        RevisionMeta(
          yesterday.minusDays(10),
          note = "kinakål",
          status = "revised"
        )
      )
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      revisionMeta = Seq(
        RevisionMeta(
          yesterday,
          note = "trylleformel",
          status = "needs-revision"
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
          today.plusDays(1),
          note = "apekatt",
          status = "needs-revision"
        ),
        RevisionMeta(
          today.plusDays(10),
          note = "note",
          status = "revised"
        )
      )
    )
    val draft2 = TestData.draft1.copy(
      id = Some(2),
      revisionMeta = Seq(
        RevisionMeta(
          today.minusDays(10),
          note = "kinakål",
          status = "revised"
        )
      )
    )
    val draft3 = TestData.draft1.copy(
      id = Some(3),
      revisionMeta = Seq(
        RevisionMeta(
          today.minusDays(10),
          note = "trylleformel",
          status = "needs-revision"
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
}
