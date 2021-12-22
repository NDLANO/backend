/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.ArticleApiClient
import no.ndla.searchapi.model.domain.article.Article
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.search.{SearchType, SearchableLanguageFormats}
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle
import org.json4s.Formats
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait ArticleIndexService {
  this: SearchConverterService with IndexService with ArticleApiClient =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends LazyLogging with IndexService[Article] {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = SearchApiProperties.SearchDocuments(SearchType.Articles)
    override val searchIndex: String = SearchApiProperties.SearchIndexes(SearchType.Articles)
    override val apiClient: ArticleApiClient = articleApiClient

    override def createIndexRequest(domainModel: Article,
                                    indexName: String,
                                    taxonomyBundle: TaxonomyBundle,
                                    grepBundle: Option[GrepBundle]): Try[IndexRequest] = {
      searchConverterService.asSearchableArticle(domainModel, taxonomyBundle, grepBundle) match {
        case Success(searchableArticle) =>
          val source = write(searchableArticle)
          Success(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))
        case Failure(ex) =>
          Failure(ex)
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
        generateLanguageSupportedDynamicTemplates("subject", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("breadcrumbs") ++
        generateLanguageSupportedDynamicTemplates("name", keepRaw = true)

      mapping(documentType).fields(fields).dynamicTemplates(dynamics)
    }
  }

}
