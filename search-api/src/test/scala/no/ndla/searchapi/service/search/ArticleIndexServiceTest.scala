/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import io.circe.syntax.*
import com.sksamuel.elastic4s.ElasticDsl.*
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain.{ArticleContent, ArticleMetaImage, Author, Description, VisualElement}
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.TestUtility.{getFields, getMappingFields}
import no.ndla.searchapi.TestData.*
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.model.search.SearchableArticle
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}

import scala.util.Success

class ArticleIndexServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))
  override val articleIndexService: ArticleIndexService = new ArticleIndexService {
    override val indexShards = 1
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    articleIndexService.deleteIndexAndAlias()
    articleIndexService.createIndexWithGeneratedName
  }

  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  test("That articles are indexed correctly") {
    articleIndexService
      .indexDocument(
        article5,
        IndexingBundle(
          Some(TestData.emptyGrepBundle),
          Some(TestData.taxonomyTestBundle),
          Some(TestData.myndlaTestBundle)
        )
      )
      .get
    articleIndexService
      .indexDocument(
        article6,
        IndexingBundle(
          Some(TestData.emptyGrepBundle),
          Some(TestData.taxonomyTestBundle),
          Some(TestData.myndlaTestBundle)
        )
      )
      .get
    articleIndexService
      .indexDocument(
        article7,
        IndexingBundle(
          Some(TestData.emptyGrepBundle),
          Some(TestData.taxonomyTestBundle),
          Some(TestData.myndlaTestBundle)
        )
      )
      .get

    blockUntil(() => { articleIndexService.countDocuments == 3 })

    val Success(response) = e4sClient.execute {
      search(articleIndexService.searchIndex)
    }

    val sources  = response.result.hits.hits.map(_.sourceAsString)
    val articles = sources.map(source => CirceUtil.unsafeParseAs[SearchableArticle](source))

    val Success(expectedArticle5) =
      searchConverterService.asSearchableArticle(
        article5,
        IndexingBundle(
          Some(TestData.emptyGrepBundle),
          Some(TestData.taxonomyTestBundle),
          Some(TestData.myndlaTestBundle)
        )
      )
    val Success(expectedArticle6) =
      searchConverterService.asSearchableArticle(
        article6,
        IndexingBundle(
          Some(TestData.emptyGrepBundle),
          Some(TestData.taxonomyTestBundle),
          Some(TestData.myndlaTestBundle)
        )
      )
    val Success(expectedArticle7) =
      searchConverterService.asSearchableArticle(
        article7,
        IndexingBundle(
          Some(TestData.emptyGrepBundle),
          Some(TestData.taxonomyTestBundle),
          Some(TestData.myndlaTestBundle)
        )
      )

    val Some(actualArticle5) = articles.find(p => p.id == article5.id.get)
    val Some(actualArticle6) = articles.find(p => p.id == article6.id.get)
    val Some(actualArticle7) = articles.find(p => p.id == article7.id.get)

    actualArticle5 should be(expectedArticle5)
    actualArticle6 should be(expectedArticle6)
    actualArticle7 should be(expectedArticle7)
  }

  test("That mapping contains every field after serialization") {
    val article = TestData.article1.copy(
      content = Seq(
        ArticleContent(
          """<section><h1>hei</h1><ndlaembed data-resource="image" data-title="heidu" data-resource_id="1"></ndlaembed><ndlaembed data-resource="h5p" data-title="yo"></ndlaembed></section>""",
          "nb"
        ),
        ArticleContent(
          """<section><h1>hei</h1><ndlaembed data-resource="image" data-title="heidu" data-resource_id="1"></ndlaembed><ndlaembed data-resource="h5p" data-title="yo"></ndlaembed></section>""",
          "en"
        )
      ),
      metaImage = List(ArticleMetaImage("hei", "hå", "nb"), ArticleMetaImage("hei", "hå", "en")),
      metaDescription = Seq(Description("hei", "nb"), Description("hei", "en")),
      visualElement = Seq(VisualElement("hei", "nb"), VisualElement("hei", "en")),
      copyright = Copyright(
        license = "CC-BY-SA-4.0",
        origin = None,
        creators = Seq(Author("writer", "hå")),
        processors = Seq(Author("writer", "hå")),
        rightsholders = Seq(Author("writer", "hå")),
        validFrom = None,
        validTo = None,
        processed = false
      )
    )

    val searchableToTestWith = searchConverterService
      .asSearchableArticle(
        article,
        IndexingBundle(
          Some(TestData.emptyGrepBundle),
          Some(TestData.taxonomyTestBundle),
          None
        )
      )
      .get

    val searchableFields = searchableToTestWith.asJson
    val fields           = getFields(searchableFields, None, Seq("domainObject"))
    val mapping          = articleIndexService.getMapping

    val staticMappingFields  = getMappingFields(mapping.properties, None)
    val dynamicMappingFields = mapping.templates.map(_.name)
    for (field <- fields) {
      val hasStatic  = staticMappingFields.contains(field)
      val hasDynamic = dynamicMappingFields.contains(field)

      if (!(hasStatic || hasDynamic)) {
        fail(s"'$field' was not found in mapping, i think you would want to add it to the index mapping?")
      }
    }
  }
}
