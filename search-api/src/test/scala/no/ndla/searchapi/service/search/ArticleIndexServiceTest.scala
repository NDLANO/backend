/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import io.circe.syntax.*
import com.sksamuel.elastic4s.ElasticDsl.*
import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.{ArticleMetaImage, Availability}
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.TestUtility.{getFields, getMappingFields}
import no.ndla.search.model.domain.EmbedValues
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.TestData.*
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.model.search.{SearchableArticle, SearchableGrepContext}
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}

import scala.util.{Failure, Success}

class ArticleIndexServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))
  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest) = {
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
    val languageValues = SearchableLanguageValues(Seq(LanguageValue("nb", "hei"), LanguageValue("en", "hå")))
    val languageList   = SearchableLanguageList(Seq(LanguageValue("nb", Seq("")), LanguageValue("en", Seq(""))))
    val now            = NDLADate.now()

    val searchableToTestWith = SearchableArticle(
      id = 10L,
      title = languageValues,
      content = languageValues,
      visualElement = languageValues,
      introduction = languageValues,
      metaDescription = languageValues,
      tags = languageList,
      lastUpdated = now,
      license = "CC-BY-SA-4.0",
      authors = List("hei", "hå"),
      articleType = "standard",
      metaImage = List(ArticleMetaImage("hei", "hå", "nb"), ArticleMetaImage("hei", "hå", "en")),
      defaultTitle = Some("hei"),
      supportedLanguages = List("nb", "en"),
      traits = List("hei"),
      embedAttributes = languageList,
      embedResourcesAndIds = List(EmbedValues(List("hei"), Some("hei"), "nb")),
      availability = Availability.everyone.toString,
      contexts = TestData.searchableTaxonomyContexts,
      grepContexts = List(
        SearchableGrepContext("KE12", None),
        SearchableGrepContext("KM123", None),
        SearchableGrepContext("TT2", None)
      )
    )

    val searchableFields = searchableToTestWith.asJson
    val fields           = getFields(searchableFields, None)
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
