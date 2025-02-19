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
        indexInto(indexName).doc(source).id(domainModel.id.get.toString)
      }
    }

    def getMapping: MappingDefinition = {
      val fields = List(
        ObjectField("domainObject", enabled = Some(false)),
        longField("id"),
        keywordField("defaultTitle"),
        dateField("lastUpdated"),
        keywordField("license"),
        textField("authors"),
        keywordField("articleType"),
        keywordField("supportedLanguages"),
        keywordField("grepContexts.code"),
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
      val dynamics = generateLanguageSupportedDynamicTemplates("title", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("metaDescription") ++
        generateLanguageSupportedDynamicTemplates("content") ++
        generateLanguageSupportedDynamicTemplates("visualElement") ++
        generateLanguageSupportedDynamicTemplates("introduction") ++
        generateLanguageSupportedDynamicTemplates("metaDescription") ++
        generateLanguageSupportedDynamicTemplates("tags") ++
        generateLanguageSupportedDynamicTemplates("embedAttributes") ++
        generateLanguageSupportedDynamicTemplates("relevance") ++
        generateLanguageSupportedDynamicTemplates("breadcrumbs") ++
        generateLanguageSupportedDynamicTemplates("name", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("context.root") ++
        generateLanguageSupportedDynamicTemplates("context.relevance") ++
        generateLanguageSupportedDynamicTemplates("context.resourceTypes.name") ++
        generateLanguageSupportedDynamicTemplates("contexts.root") ++
        generateLanguageSupportedDynamicTemplates("contexts.relevance") ++
        generateLanguageSupportedDynamicTemplates("contexts.resourceTypes.name")

      properties(fields).dynamicTemplates(dynamics)
    }
  }

}
