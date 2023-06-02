/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.domain.ArticleContent
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.TestData.{core, generateContexts, subjectMaterial}
import no.ndla.searchapi.model.taxonomy._
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
  override val multiSearchService     = new MultiSearchService
  override val converterService       = new ConverterService
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
          s"""<section><div data-type="related-content"><$EmbedTagName data-article-id="3" data-resource="related-content"></div></section>""",
          "nb"
        )
      )
    )
    val article2 = TestData.article1.copy(
      id = Some(2),
      content = Seq(
        ArticleContent(
          s"""<section><$EmbedTagName data-content-id="3" data-resource="content-link">Test?</$EmbedTagName></section>""",
          "nb"
        )
      )
    )
    val article3 = TestData.article1.copy(id = Some(3))
    articleIndexService.indexDocument(article1, Some(TestData.taxonomyTestBundle), Some(TestData.grepBundle)).get
    articleIndexService.indexDocument(article2, Some(TestData.taxonomyTestBundle), Some(TestData.grepBundle)).get
    articleIndexService.indexDocument(article3, Some(TestData.taxonomyTestBundle), Some(TestData.grepBundle)).get

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

  test("That resource taxonomy contexts with hidden elements are ignored") {
    val article1 = TestData.article1.copy(id = Some(1))

    val taxonomyBundle = {
      val visibleMeta = Some(Metadata(List.empty, visible = true, Map.empty))
      val hiddenMeta  = Some(Metadata(List.empty, visible = false, Map.empty))

      // Visible subject
      val subject_1 = Node(
        "urn:subject:1",
        "Sub1",
        None,
        Some("/subject:1"),
        visibleMeta,
        List.empty,
        NodeType.SUBJECT,
        List(
          TaxonomyContext(
            publicId = "urn:subject:1",
            rootId = "urn:subject:1",
            root = SearchableLanguageValues(Seq(LanguageValue("nb", "Sub1"))),
            path = "/subject:1",
            breadcrumbs = SearchableLanguageList(Seq(LanguageValue("nb", Seq.empty))),
            contextType = None,
            relevanceId = None,
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
      // Hidden topic with visible subject
      val topic_1 = Node(
        "urn:topic:1",
        "Top1",
        Some("urn:article:2"),
        Some("/subject:1/topic:1"),
        hiddenMeta,
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
        Some(core),
        isPrimary = true,
        isVisible = false,
        isActive = true
      )
      // Visible subtopic
      val topic_2 = Node(
        "urn:topic:2",
        "Top2",
        Some("urn:article:3"),
        Some("/subject:1/topic:1/topic:2"),
        visibleMeta,
        List.empty,
        NodeType.TOPIC,
        List.empty
      )
      topic_2.contexts = generateContexts(
        topic_2,
        subject_1,
        topic_1,
        List.empty,
        None,
        Some(core),
        isPrimary = true,
        isVisible = false,
        isActive = true
      )
      // Visible topic
      val topic_3 = Node(
        "urn:topic:3",
        "Top3",
        Some("urn:article:4"),
        Some("/subject:1/topic:3"),
        visibleMeta,
        List.empty,
        NodeType.TOPIC,
        List.empty
      )
      topic_3.contexts = generateContexts(
        topic_3,
        subject_1,
        subject_1,
        List.empty,
        None,
        Some(core),
        isPrimary = true,
        isVisible = true,
        isActive = true
      )
      // Visible resource with hidden parent topic
      val resource_1 = Node(
        "urn:resource:1",
        "Res1",
        Some("urn:article:1"),
        Some("/subject:1/topic:1/topic:2/resource:1"),
        visibleMeta,
        List.empty,
        NodeType.RESOURCE,
        List.empty
      )
      resource_1.contexts = generateContexts(
        resource_1,
        subject_1,
        topic_2,
        List(subjectMaterial),
        None,
        Some(core),
        isPrimary = true,
        isVisible = false,
        isActive = true
      )
      // Visible resource with visible parent topic
      val resource_2 = Node(
        "urn:resource:2",
        "Res2",
        Some("urn:article:1"),
        Some("/subject:1/topic:3/resource:2"),
        visibleMeta,
        List.empty,
        NodeType.RESOURCE,
        List.empty
      )
      resource_2.contexts = generateContexts(
        resource_2,
        subject_1,
        topic_3,
        List(subjectMaterial),
        None,
        Some(core),
        isPrimary = true,
        isVisible = true,
        isActive = true
      )

      val nodes = List(
        resource_1,
        resource_2,
        topic_1,
        topic_2,
        topic_3,
        subject_1
      )

      TaxonomyBundle(nodes = nodes)
    }

    articleIndexService.indexDocument(article1, Some(taxonomyBundle), Some(TestData.grepBundle)).get

    blockUntil(() => {
      articleIndexService.countDocuments == 1
    })

    val result = multiSearchService
      .matchingQuery(
        TestData.searchSettings.copy(
        )
      )
      .get

    result.results.head.contexts.map(_.id) should be(Seq("urn:resource:2"))
  }

  test("That topic taxonomy contexts with hidden elements are ignored") {
    val article1 = TestData.article1.copy(id = Some(1))

    val taxonomyBundle = {
      val visibleMeta = Some(Metadata(List.empty, visible = true, Map.empty))
      val hiddenMeta  = Some(Metadata(List.empty, visible = false, Map.empty))

      // Visible subject
      val subject_1 = Node(
        "urn:subject:1",
        "Sub1",
        None,
        Some("/subject:1"),
        visibleMeta,
        List.empty,
        NodeType.SUBJECT,
        List(
          TaxonomyContext(
            publicId = "urn:subject:1",
            rootId = "urn:subject:1",
            root = SearchableLanguageValues(Seq(LanguageValue("nb", "Sub1"))),
            path = "/subject:1",
            breadcrumbs = SearchableLanguageList(Seq(LanguageValue("nb", Seq.empty))),
            contextType = None,
            relevanceId = None,
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
      // Hidden topic with visible subject
      val topic_1 = Node(
        "urn:topic:1",
        "Top1",
        Some("urn:article:1"),
        Some("/subject:1/topic:1"),
        hiddenMeta,
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
        Some(core),
        isPrimary = true,
        isVisible = false,
        isActive = true
      ) // TODO: use visible from node also
      // Visible subtopic
      val topic_2 = Node(
        "urn:topic:2",
        "Top1",
        Some("urn:article:1"),
        Some("/subject:1/topic:1/topic:2"),
        visibleMeta,
        List.empty,
        NodeType.TOPIC,
        List.empty
      )
      topic_2.contexts = generateContexts(
        topic_2,
        subject_1,
        topic_1,
        List.empty,
        None,
        Some(core),
        isPrimary = true,
        isVisible = true,
        isActive = true
      )
      // Visible topic
      val topic_3 = Node(
        "urn:topic:3",
        "Top1",
        Some("urn:article:1"),
        Some("/subject:1/topic:3"),
        visibleMeta,
        List.empty,
        NodeType.TOPIC,
        List.empty
      )
      topic_3.contexts = generateContexts(
        topic_3,
        subject_1,
        subject_1,
        List.empty,
        None,
        Some(core),
        isPrimary = true,
        isVisible = true,
        isActive = true
      )

      val nodes = List(
        topic_1,
        topic_2,
        topic_3,
        subject_1
      )

      TaxonomyBundle(nodes = nodes)
    }

    articleIndexService.indexDocument(article1, Some(taxonomyBundle), Some(TestData.grepBundle)).get

    blockUntil(() => {
      articleIndexService.countDocuments == 1
    })

    val result = multiSearchService
      .matchingQuery(
        TestData.searchSettings.copy(
        )
      )
      .get

    result.results.head.contexts.map(_.id) should be(Seq("urn:topic:3"))
  }
}
