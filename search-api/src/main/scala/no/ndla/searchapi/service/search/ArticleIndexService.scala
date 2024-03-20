/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.api.MyNDLABundle
import no.ndla.common.model.domain.article.Article
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.ArticleApiClient
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle

import scala.util.Try

trait ArticleIndexService {
  this: SearchConverterService with IndexService with ArticleApiClient with Props =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends StrictLogging with IndexService[Article] {
    override val documentType: String        = props.SearchDocuments(SearchType.Articles)
    override val searchIndex: String         = props.SearchIndexes(SearchType.Articles)
    override val apiClient: ArticleApiClient = articleApiClient

    override def createIndexRequest(
        domainModel: Article,
        indexName: String,
        taxonomyBundle: Option[TaxonomyBundle],
        grepBundle: Option[GrepBundle],
        myndlaBundle: Option[MyNDLABundle]
    ): Try[IndexRequest] = {
      searchConverterService.asSearchableArticle(domainModel, taxonomyBundle, grepBundle).map { searchableArticle =>
        val source = CirceUtil.toJsonString(searchableArticle)
        indexInto(indexName).doc(source).id(domainModel.id.get.toString)
      }
    }

    def getMapping: MappingDefinition = {
      val fields = List(
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
        getTaxonomyContextMapping,
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
        generateLanguageSupportedDynamicTemplates("contexts.root") ++
        generateLanguageSupportedDynamicTemplates("contexts.relevance") ++
        generateLanguageSupportedDynamicTemplates("contexts.resourceTypes.name")

      properties(fields).dynamicTemplates(dynamics)
    }
  }

}
