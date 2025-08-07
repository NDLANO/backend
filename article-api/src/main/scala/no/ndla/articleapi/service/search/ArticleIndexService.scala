/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.Props
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.article.Article
import scala.util.{Failure, Success, Try}
import no.ndla.search.model.domain.{ReindexResult, BulkIndexResult}

trait ArticleIndexService {
  this: SearchConverterService & IndexService & ArticleRepository & Props =>
  lazy val articleIndexService: ArticleIndexService

  class ArticleIndexService extends StrictLogging {
    val documentType: String = ???
    val searchIndex: String  = ???

    def createIndexRequest(domainModel: Article, indexName: String): IndexRequest = {
      ???
      //   val searchable = searchConverterService.asSearchableArticle(domainModel)
      //   val source     = CirceUtil.toJsonString(searchable)
      //   indexInto(indexName).doc(source).id(domainModel.id.get.toString)
    }

    type SendToElastic = String => Try[BulkIndexResult]
    def indexDocument(imported: Article): Try[Article] = ???
    def indexDocumentsInBulk(numShards: Option[Int])(
        sendToElasticFunction: SendToElastic
    ): Try[ReindexResult] = ???
    def createIndexAndAlias(): Try[String]                            = ???
    def createIndexAndAlias(numberOfShards: Option[Int]): Try[String] = ???
    def indexDocuments(numShards: Option[Int]): Try[ReindexResult]    = ???
    def deleteDocument(contentId: Long): Try[Long]                    = ???
    def findAllIndexes(indexName: String): Try[Seq[String]]           = ???
    def deleteIndexWithName(optIndexName: Option[String]): Try[?]     = ???
    def getMapping: MappingDefinition                                 = ???
  }

}
