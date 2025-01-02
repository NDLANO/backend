/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import cats.implicits.toTraverseOps
import no.ndla.common.implicits.TryQuestionMark
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.search.model.domain.{BulkIndexResult, ReindexResult}
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.GrepApiClient
import no.ndla.searchapi.model.grep.{GrepBundle, GrepElement}
import no.ndla.searchapi.model.search.SearchType

import scala.util.{Success, Try}

trait GrepIndexService {
  this: SearchConverterService & IndexService & Props & GrepApiClient =>
  val grepIndexService: GrepIndexService

  class GrepIndexService extends BulkIndexingService with StrictLogging {
    import props.SearchIndex
    override val documentType: String       = "grep"
    override val searchIndex: String        = SearchIndex(SearchType.Grep)
    override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

    override def getMapping: MappingDefinition = {
      val fields = List(
        keywordField("defaultTitle"),
        keywordField("code").normalizer("lower"),
        keywordField("laereplanCode").normalizer("lower")
      )

      val dynamics = generateLanguageSupportedDynamicTemplates("title", keepRaw = true)
      properties(fields).dynamicTemplates(dynamics)
    }

    def indexDocuments(numShards: Option[Int], grepBundle: Option[GrepBundle]): Try[ReindexResult] = {
      indexDocumentsInBulk(numShards) { indexName =>
        sendToElastic(grepBundle, indexName)
      }
    }

    def createIndexRequest(grepElement: GrepElement, indexName: String): Try[IndexRequest] = {
      val searchable = searchConverterService.asSearchableGrep(grepElement).?
      val source     = CirceUtil.toJsonString(searchable)
      Success(indexInto(indexName).doc(source).id(grepElement.kode))
    }

    private def sendChunkToElastic(chunk: List[GrepElement], indexName: String): Try[BulkIndexResult] = {
      chunk
        .traverse(grepElement => createIndexRequest(grepElement, indexName))
        .map(executeRequests)
        .flatten
    }

    def sendToElastic(grepBundle: Option[GrepBundle], indexName: String): Try[BulkIndexResult] = {
      val bundle = (grepBundle match {
        case Some(value) => Success(value)
        case None        => grepApiClient.getGrepBundle()
      }).?

      bundle.grepContext
        .grouped(props.IndexBulkSize)
        .toList
        .traverse(group => sendChunkToElastic(group, indexName))
        .map(countBulkIndexed)
    }
  }
}
