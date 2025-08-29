/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import cats.implicits.toTraverseOps
import no.ndla.common.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.ObjectField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.api.search.SearchType
import no.ndla.search.{NdlaE4sClient, SearchLanguage}
import no.ndla.search.model.domain.{BulkIndexResult, ReindexResult}
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.GrepApiClient
import no.ndla.searchapi.model.grep.{GrepBundle, GrepElement}

import scala.util.{Success, Try}

class GrepIndexService(using
    searchConverterService: SearchConverterService,
    props: Props,
    grepApiClient: GrepApiClient,
    e4sClient: NdlaE4sClient,
    searchLanguage: SearchLanguage
) extends BulkIndexingService
    with StrictLogging {
  override val documentType: String       = "grep"
  override val searchIndex: String        = props.SearchIndex(SearchType.Grep)
  override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

  override def getMapping: MappingDefinition = {
    val fields = List(
      keywordField("defaultTitle"),
      keywordField("code").normalizer("lower"),
      keywordField("status"),
      keywordField("laereplanCode").normalizer("lower"),
      keywordField("gjenbrukAv").normalizer("lower"),
      keywordField("erstattesAv").normalizer("lower"),
      ObjectField("domainObject", enabled = Some(false))
    )

    val dynamics = languageValuesMapping("title", keepRaw = true)
    properties(fields ++ dynamics)
  }

  def indexDocuments(numShards: Option[Int], grepBundle: Option[GrepBundle]): Try[ReindexResult] = {
    indexDocumentsInBulk(numShards) { indexName =>
      sendToElastic(grepBundle, indexName)
    }
  }

  def createIndexRequest(grepElement: GrepElement, indexName: String): Try[IndexRequest] = permitTry {
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

  def sendToElastic(grepBundle: Option[GrepBundle], indexName: String): Try[BulkIndexResult] = permitTry {
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
