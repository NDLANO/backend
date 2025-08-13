/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.ElasticField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.Props
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.common.model.domain.article.Article
import no.ndla.search.model.domain.{BulkIndexResult, ReindexResult}
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient & BaseIndexService & Props & ArticleRepository & SearchLanguage =>

  abstract class IndexService extends BaseIndexService with StrictLogging {
    override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

    def createIndexRequest(domainModel: Article, indexName: String): IndexRequest

    def indexDocument(imported: Article): Try[Article] = {
      for {
        _ <- createIndexIfNotExists()
        _ <- e4sClient.execute {
          createIndexRequest(imported, searchIndex)
        }
      } yield imported
    }

    def indexDocuments(numShards: Option[Int]): Try[ReindexResult] = synchronized {
      indexDocumentsInBulk(numShards) {
        sendToElastic
      }
    }

    def sendToElastic(indexName: String): Try[BulkIndexResult] = {
      getRanges
        .flatMap(ranges => {
          ranges.traverse { case (start, end) =>
            val toIndex = articleRepository.documentsWithIdBetween(start, end)
            indexDocuments(toIndex, indexName)
          }
        })
        .map(countBulkIndexed)
    }

    def getRanges: Try[List[(Long, Long)]] = {
      Try {
        val (minId, maxId) = articleRepository.minMaxId
        Seq
          .range(minId, maxId + 1)
          .grouped(props.IndexBulkSize)
          .map(group => (group.head, group.last))
          .toList
      }
    }

    def indexDocuments(contents: Seq[Article], indexName: String): Try[BulkIndexResult] = {
      if (contents.isEmpty) {
        Success(BulkIndexResult.empty)
      } else {
        val response = e4sClient.execute {
          bulk(contents.map(content => {
            createIndexRequest(content, indexName)
          }))
        }

        response match {
          case Success(r) =>
            logger.info(s"Indexed ${contents.size} documents. No of failed items: ${r.result.failures.size}")
            Success(BulkIndexResult(r.result.successes.size, contents.size))
          case Failure(ex) => Failure(ex)
        }
      }
    }

    /** Returns Sequence of FieldDefinitions for a given field.
      *
      * @param fieldName
      *   Name of field in mapping.
      * @param keepRaw
      *   Whether to add a keywordField named raw. Usually used for sorting, aggregations or scripts.
      * @return
      *   Sequence of FieldDefinitions for a field.
      */
    protected def generateLanguageSupportedFieldList(fieldName: String, keepRaw: Boolean = false): Seq[ElasticField] = {
      if (keepRaw) {
        SearchLanguage.languageAnalyzers.map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
            .fielddata(false)
            .analyzer(langAnalyzer.analyzer)
            .fields(keywordField("raw"))
        )
      } else {
        SearchLanguage.languageAnalyzers.map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
            .fielddata(false)
            .analyzer(langAnalyzer.analyzer)
        )
      }
    }
  }
}
