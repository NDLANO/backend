/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.article.Article
import no.ndla.search.model.SearchableLanguageFormats
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.ArticleApiClient
import no.ndla.searchapi.model.grep.GrepBundle
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.model.taxonomy.TaxonomyBundle
import org.json4s.Formats
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait ArticleIndexService {
  this: SearchConverterService with IndexService with ArticleApiClient with Props =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends StrictLogging with IndexService[Article] {
    implicit val formats: Formats            = SearchableLanguageFormats.JSonFormatsWithMillis
    override val documentType: String        = props.SearchDocuments(SearchType.Articles)
    override val searchIndex: String         = props.SearchIndexes(SearchType.Articles)
    override val apiClient: ArticleApiClient = articleApiClient

    override def createIndexRequest(
        domainModel: Article,
        indexName: String,
        taxonomyBundle: Option[TaxonomyBundle],
        grepBundle: Option[GrepBundle]
    ): Try[IndexRequest] = {
      searchConverterService.asSearchableArticle(domainModel, taxonomyBundle, grepBundle) match {
        case Success(searchableArticle) =>
          val source = Try(write(searchableArticle))
          source.map(s => indexInto(indexName).doc(s).id(domainModel.id.get.toString))
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
