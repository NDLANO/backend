/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.Elastic4sClientFactory
import no.ndla.searchapi.TestData.blockUntil
import no.ndla.searchapi.model.domain.article._
import no.ndla.searchapi.model.taxonomy.{
  Metadata,
  Resource,
  ResourceResourceTypeConnection,
  SubjectTopicConnection,
  TaxSubject,
  TaxonomyBundle,
  Topic,
  TopicResourceConnection,
  TopicSubtopicConnection
}
import no.ndla.searchapi.{TestData, TestEnvironment}
import org.scalatest.Outcome

import scala.util.{Failure, Success}

class MultiSearchServiceAtomicTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {
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
  override val multiSearchService = new MultiSearchService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

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
    val article1 = TestData.article1.copy(
      id = Some(1),
      content = Seq(
        ArticleContent(
          """<section><div data-type="related-content"><embed data-article-id="3" data-resource="related-content"></div></section>""",
          "nb"
        )
      )
    )
    val article2 = TestData.article1.copy(
      id = Some(2),
      content = Seq(
        ArticleContent(
          """<section><embed data-content-id="3" data-link-text="Test?" data-resource="content-link"></section>""",
          "nb"
        )
      )
    )
    val article3 = TestData.article1.copy(id = Some(3))
    articleIndexService.indexDocument(article1, TestData.taxonomyTestBundle, Some(TestData.grepBundle)).get
    articleIndexService.indexDocument(article2, TestData.taxonomyTestBundle, Some(TestData.grepBundle)).get
    articleIndexService.indexDocument(article3, TestData.taxonomyTestBundle, Some(TestData.grepBundle)).get

    blockUntil(() => {
      articleIndexService.countDocuments == 3
    })

    val Success(search1) =
      multiSearchService.matchingQuery(
        TestData.searchSettings.copy(embedId = Some("3"), embedResource = List("content-link"))
      )

    search1.totalCount should be(1)
    search1.results.map(_.id) should be(List(2))

    val Success(search2) =
      multiSearchService.matchingQuery(
        TestData.searchSettings.copy(embedId = Some("3"), embedResource = List("content-link", "related-content"))
      )

    search2.totalCount should be(2)
    search2.results.map(_.id) should be(List(1, 2))

  }

  test("That taxonomy contexts with hidden elements are ignored") {
    val article1 = TestData.article1.copy(
      id = Some(1),
      content = Seq(
        ArticleContent(
          """<section><div data-type="related-content"><embed data-article-id="3" data-resource="related-content"></div></section>""",
          "nb"
        )
      )
    )

    val visibleMeta = Some(Metadata(List.empty, visible = true))
    val hiddenMeta = Some(Metadata(List.empty, visible = false))

    val resources = List(
      // Visible resource with hidden parent topic
      Resource(
        "urn:resource:1",
        "Res1",
        Some("urn:article:1"),
        Some("/subject:1/topic:1/topic:2/resource:1"),
        visibleMeta,
        List.empty
      ),
      // Visible resource with visible parent topic
      Resource(
        "urn:resource:2",
        "Res2",
        Some("urn:article:1"),
        Some("/subject:1/topic:3/resource:2"),
        visibleMeta,
        List.empty
      )
    )

    val topics = List(
      // Hidden topic with visible subject
      Topic(
        "urn:topic:1",
        "Top1",
        Some("urn:article:2"),
        Some("/subject:1/topic:1"),
        hiddenMeta,
        List.empty
      ),
      // Visible subtopic
      Topic(
        "urn:topic:2",
        "Top1",
        Some("urn:article:3"),
        Some("/subject:1/topic:1/topic:2"),
        visibleMeta,
        List.empty
      ),
      // Visible topic
      Topic(
        "urn:topic:3",
        "Top1",
        Some("urn:article:4"),
        Some("/subject:1/topic:3"),
        visibleMeta,
        List.empty
      )
    )

    val subjects = List(
      // Visible subject
      TaxSubject(
        "urn:subject:1",
        "Sub1",
        None,
        Some("/subject:1"),
        visibleMeta,
        List.empty
      )
    )

    val resourceResourceTypeConnections = List(
      ResourceResourceTypeConnection("urn:resource:1",
                                     "urn:resourcetype:subjectMaterial",
                                     "urn:resourceresourcetype:1"),
      ResourceResourceTypeConnection("urn:resource:2", "urn:resourcetype:subjectMaterial", "urn:resourceresourcetype:1")
    )

    val subjectTopicConnections = List(
      SubjectTopicConnection(
        "urn:subject:1",
        "urn:topic:1",
        "urn:subjecttopic:1",
        primary = true,
        1,
        Some("urn:relevance:core")
      ),
      SubjectTopicConnection(
        "urn:subject:1",
        "urn:topic:3",
        "urn:subjecttopic:2",
        primary = true,
        1,
        Some("urn:relevance:core")
      )
    )

    val topicSubtopicConnections = List(
      TopicSubtopicConnection(
        "urn:topic:1",
        "urn:topic:2",
        "urn:topicsubtopic:1",
        primary = true,
        1,
        Some("urn:relevance:core")
      )
    )

    val topicResourceConnections = List(
      TopicResourceConnection(
        "urn:topic:2",
        "urn:resource:1",
        "urn:topicresource:1",
        primary = true,
        1,
        Some("urn:relevance:core")
      ),
      TopicResourceConnection(
        "urn:topic:3",
        "urn:resource:2",
        "urn:topicresource:2",
        primary = true,
        1,
        Some("urn:relevance:core")
      )
    )

    val taxonomyBundle = TaxonomyBundle(
      resources = resources,
      topics = topics,
      subjects = subjects,
      relevances = TestData.relevances,
      resourceResourceTypeConnections = resourceResourceTypeConnections,
      resourceTypes = TestData.resourceTypes,
      subjectTopicConnections = subjectTopicConnections,
      topicResourceConnections = topicResourceConnections,
      topicSubtopicConnections = topicSubtopicConnections,
    )

    articleIndexService.indexDocument(article1, taxonomyBundle, Some(TestData.grepBundle)).get

    blockUntil(() => {
      articleIndexService.countDocuments == 1
    })

    val result = multiSearchService
      .matchingQuery(
        TestData.searchSettings.copy(
          ))
      .get

    result.results.head.contexts.map(_.id) should be(Seq("urn:resource:2"))
  }
}
