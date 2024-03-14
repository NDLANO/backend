/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import no.ndla.common.CirceUtil
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.Props
import no.ndla.articleapi.model.search.SearchableArticle
import no.ndla.articleapi.repository.{ArticleRepository, Repository}
import no.ndla.common.model.domain.article.Article

trait ArticleIndexService {
  this: SearchConverterService with IndexService with ArticleRepository with Props =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends StrictLogging with IndexService[Article, SearchableArticle] {
    override val documentType: String            = props.ArticleSearchDocument
    override val searchIndex: String             = props.ArticleSearchIndex
    override val repository: Repository[Article] = articleRepository

    override def createIndexRequest(domainModel: Article, indexName: String): IndexRequest = {
      val searchable = searchConverterService.asSearchableArticle(domainModel)
      val source     = CirceUtil.toJsonString(searchable)
      indexInto(indexName).doc(source).id(domainModel.id.get.toString)
    }

    def getMapping: MappingDefinition = {
      val fields = List(
        intField("id"),
        keywordField("defaultTitle"),
        dateField("lastUpdated"),
        keywordField("license"),
        keywordField("availability"),
        textField("authors").fielddata(true),
        textField("articleType").analyzer("keyword"),
        nestedField("metaImage").fields(
          keywordField("imageId"),
          keywordField("altText"),
          keywordField("language")
        ),
        keywordField("grepCodes")
      )
      val dynamics = generateLanguageSupportedDynamicTemplates("title", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("content") ++
        generateLanguageSupportedDynamicTemplates("visualElement") ++
        generateLanguageSupportedDynamicTemplates("introduction") ++
        generateLanguageSupportedDynamicTemplates("metaDescription") ++
        generateLanguageSupportedDynamicTemplates("tags")

      properties(fields).dynamicTemplates(dynamics)
    }
  }

}
