/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.ObjectField
import com.sksamuel.elastic4s.requests.common.VersionType.EXTERNAL_GTE
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.api.search.SearchType
import no.ndla.common.model.domain.article.Article
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.ArticleApiClient
import no.ndla.searchapi.model.domain.IndexingBundle

import scala.util.Try

trait ArticleIndexService {
  this: SearchConverterService & IndexService & ArticleApiClient & Props =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends StrictLogging with IndexService[Article] {
    import props.SearchIndex
    override val documentType: String        = "article"
    override val searchIndex: String         = SearchIndex(SearchType.Articles)
    override val apiClient: ArticleApiClient = articleApiClient

    override def createIndexRequest(
        domainModel: Article,
        indexName: String,
        indexingBundle: IndexingBundle
    ): Try[IndexRequest] = {
      searchConverterService.asSearchableArticle(domainModel, indexingBundle).map { searchableArticle =>
        val source = CirceUtil.toJsonString(searchableArticle)
        indexInto(indexName)
          .doc(source)
          .id(domainModel.id.get.toString)
          .versionType(EXTERNAL_GTE)
          .version(domainModel.revision.map(_.toLong).get)
      }
    }

    def getMapping: MappingDefinition = {
      val fields = List(
        ObjectField("domainObject", enabled = Some(false)),
        longField("id"),
        keywordField("defaultTitle"),
        textField("typeName"),
        dateField("lastUpdated"),
        keywordField("license"),
        keywordField("status"),
        textField("authors"),
        keywordField("articleType"),
        keywordField("supportedLanguages"),
        keywordField("grepContexts.code"),
        keywordField("grepContexts.status"),
        textField("grepContexts.title"),
        keywordField("traits"),
        keywordField("availability"),
        keywordField("learningResourceType"),
        getTaxonomyContextMapping("context"),
        getTaxonomyContextMapping("contexts"),
        keywordField("contextids"),
        nestedField("embedResourcesAndIds").fields(
          keywordField("resource"),
          keywordField("id"),
          keywordField("language")
        ),
        nestedField("metaImage").fields(
          keywordField("imageId"),
          keywordField("altText"),
          keywordField("language")
        ),
        dateField("nextRevision.revisionDate") // This is needed for sorting, even if it is never used for articles
      )
      val dynamics =
        languageValuesMapping("title", keepRaw = true) ++
          languageValuesMapping("metaDescription") ++
          languageValuesMapping("content") ++
          languageValuesMapping("introduction") ++
          languageValuesMapping("tags") ++
          languageValuesMapping("embedAttributes") ++
          languageValuesMapping("relevance") ++
          languageValuesMapping("breadcrumbs") ++
          languageValuesMapping("name", keepRaw = true)

      properties(fields ++ dynamics)
    }
  }

}
