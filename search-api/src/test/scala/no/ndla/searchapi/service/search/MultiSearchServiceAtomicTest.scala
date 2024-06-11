/*
 * Part of NDLA search-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.domain.ArticleContent
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.model.domain.{Bucket, TermAggregation}
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.TestData.{core, generateContexts, subjectMaterial}
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.model.taxonomy.*
import no.ndla.searchapi.{TestData, TestEnvironment}

import scala.util.Success

class MultiSearchServiceAtomicTest extends IntegrationSuite(EnableElasticsearchContainer = true) with TestEnvironment {
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

  val indexingBundle: IndexingBundle =
    IndexingBundle(Some(TestData.grepBundle), Some(TestData.taxonomyTestBundle), Some(TestData.myndlaTestBundle))

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
    articleIndexService.indexDocument(article1, indexingBundle).get
    articleIndexService.indexDocument(article2, indexingBundle).get
    articleIndexService.indexDocument(article3, indexingBundle).get

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
            relevanceId = core.id,
            relevance = SearchableLanguageValues(Seq.empty),
            resourceTypes = List.empty,
            parentIds = List.empty,
            isPrimary = true,
            contextId = "",
            isVisible = true,
            isActive = true,
            None
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
        core,
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
        core,
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
        core,
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
        core,
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
        core,
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

    articleIndexService
      .indexDocument(
        article1,
        IndexingBundle(
          Some(TestData.grepBundle),
          Some(taxonomyBundle),
          Some(TestData.myndlaTestBundle)
        )
      )
      .get

    blockUntil(() => {
      articleIndexService.countDocuments == 1
    })

    val result = multiSearchService
      .matchingQuery(
        TestData.searchSettings.copy(
        )
      )
      .get

    result.results.head.contexts.map(_.publicId) should be(Seq("urn:resource:2"))
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
            relevanceId = core.id,
            relevance = SearchableLanguageValues(Seq.empty),
            resourceTypes = List.empty,
            parentIds = List.empty,
            isPrimary = true,
            contextId = "",
            isVisible = true,
            isActive = true,
            None
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
        core,
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
        core,
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
        core,
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

    articleIndexService
      .indexDocument(
        article1,
        IndexingBundle(
          Some(TestData.grepBundle),
          Some(taxonomyBundle),
          Some(TestData.myndlaTestBundle)
        )
      )
      .get

    blockUntil(() => {
      articleIndexService.countDocuments == 1
    })

    val result = multiSearchService
      .matchingQuery(
        TestData.searchSettings.copy(
        )
      )
      .get

    result.results.head.contexts.map(_.publicId) should be(Seq("urn:topic:3"))
  }
  test("That aggregating rootId works as expected") {
    val article1 = TestData.article1.copy(id = Some(1))
    val article2 = TestData.article1.copy(id = Some(2))
    val article3 = TestData.article1.copy(id = Some(3))
    val article4 = TestData.article1.copy(id = Some(4))
    val article5 = TestData.article1.copy(id = Some(5))

    val taxonomyBundle = {
      val visibleMeta = Some(Metadata(List.empty, visible = true, Map.empty))
      val hiddenMeta  = Some(Metadata(List.empty, visible = false, Map.empty))

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
            relevanceId = core.id,
            relevance = SearchableLanguageValues(Seq.empty),
            resourceTypes = List.empty,
            parentIds = List.empty,
            isPrimary = true,
            contextId = "",
            isVisible = true,
            isActive = true,
            None
          )
        )
      )
      val subject_2 = Node(
        "urn:subject:2",
        "Sub2",
        None,
        Some("/subject:2"),
        visibleMeta,
        List.empty,
        NodeType.SUBJECT,
        List(
          TaxonomyContext(
            publicId = "urn:subject:2",
            rootId = "urn:subject:2",
            root = SearchableLanguageValues(Seq(LanguageValue("nb", "Sub2"))),
            path = "/subject:2",
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
            None
          )
        )
      )
      val topic_1 = Node(
        "urn:topic:1",
        "Top1",
        Some(s"urn:article:${article1.id.get}"),
        Some(s"${subject_1.path.get}/topic:1"),
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
        core,
        isPrimary = true,
        isVisible = true,
        isActive = true
      )
      val topic_2 = Node(
        "urn:topic:2",
        "Top2",
        Some(s"urn:article:${article2.id.get}"),
        Some(s"${subject_1.path.get}/topic:2"),
        hiddenMeta,
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
        "Top3",
        Some(s"urn:article:${article3.id.get}"),
        Some(s"${subject_1.path.get}/topic:3"),
        hiddenMeta,
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
        core,
        isPrimary = true,
        isVisible = true,
        isActive = true
      )
      val topic_4 = Node(
        "urn:topic:4",
        "Top4",
        Some(s"urn:article:${article4.id.get}"),
        Some(s"${subject_2.path.get}/topic:4"),
        hiddenMeta,
        List.empty,
        NodeType.TOPIC,
        List.empty
      )
      topic_4.contexts = generateContexts(
        topic_4,
        subject_2,
        subject_2,
        List.empty,
        None,
        core,
        isPrimary = true,
        isVisible = true,
        isActive = true
      )
      val topic_5 = Node(
        "urn:topic:5",
        "Top5",
        Some(s"urn:article:${article5.id.get}"),
        Some(s"${subject_2.path.get}/topic:5"),
        hiddenMeta,
        List.empty,
        NodeType.TOPIC,
        List.empty
      )
      topic_5.contexts = generateContexts(
        topic_5,
        subject_2,
        subject_2,
        List.empty,
        None,
        core,
        isPrimary = true,
        isVisible = true,
        isActive = true
      )

      val nodes = List(
        topic_1,
        topic_2,
        topic_3,
        topic_4,
        topic_5,
        subject_1,
        subject_2
      )

      TaxonomyBundle(nodes = nodes)
    }

    articleIndexService
      .indexDocument(
        article1,
        IndexingBundle(Some(TestData.grepBundle), Some(taxonomyBundle), Some(TestData.myndlaTestBundle))
      )
      .get
    articleIndexService
      .indexDocument(
        article2,
        IndexingBundle(Some(TestData.grepBundle), Some(taxonomyBundle), Some(TestData.myndlaTestBundle))
      )
      .get
    articleIndexService
      .indexDocument(
        article3,
        IndexingBundle(Some(TestData.grepBundle), Some(taxonomyBundle), Some(TestData.myndlaTestBundle))
      )
      .get
    articleIndexService
      .indexDocument(
        article4,
        IndexingBundle(Some(TestData.grepBundle), Some(taxonomyBundle), Some(TestData.myndlaTestBundle))
      )
      .get
    articleIndexService
      .indexDocument(
        article5,
        IndexingBundle(Some(TestData.grepBundle), Some(taxonomyBundle), Some(TestData.myndlaTestBundle))
      )
      .get

    blockUntil(() => {
      articleIndexService.countDocuments == 5
    })

    val result = multiSearchService
      .matchingQuery(
        TestData.searchSettings.copy(
          aggregatePaths = List("contexts.rootId")
        )
      )
      .get

    val expectedAggs =
      TermAggregation(
        field = List("contexts", "rootId"),
        sumOtherDocCount = 0,
        docCountErrorUpperBound = 0,
        buckets = List(Bucket("urn:subject:1", 3), Bucket("urn:subject:2", 2))
      )
    result.aggregations should be(Seq(expectedAggs))
  }
}
