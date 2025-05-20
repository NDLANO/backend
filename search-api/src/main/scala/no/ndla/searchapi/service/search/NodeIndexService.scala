/*
 * Part of NDLA search-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticApi.*
import com.sksamuel.elastic4s.fields.ObjectField
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.implicits.*
import no.ndla.search.model.domain.{BulkIndexResult, ReindexResult}
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.{ArticleApiClient, GrepApiClient, TaxonomyApiClient}
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.model.taxonomy.Node
import cats.implicits.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import no.ndla.common.model.api.search.SearchType
import no.ndla.common.model.domain.frontpage.SubjectPage
import no.ndla.common.{CirceUtil, ContentURIUtil}
import no.ndla.network.clients.FrontpageApiClient
import no.ndla.network.model.HttpRequestException

import scala.util.{Failure, Success, Try}

trait NodeIndexService {
  this: SearchConverterService & IndexService & Props & TaxonomyApiClient & ArticleApiClient & FrontpageApiClient &
    GrepApiClient =>
  val nodeIndexService: NodeIndexService
  class NodeIndexService extends StrictLogging with BulkIndexingService {
    import props.SearchIndex
    override val documentType: String       = "nodes"
    override val searchIndex: String        = SearchIndex(SearchType.Nodes)
    override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

    override def getMapping: MappingDefinition = {
      val fields = List(
        keywordField("nodeId"),
        keywordField("contentUri"),
        keywordField("nodeType"),
        keywordField("url"),
        textField("typeName"),
        keywordField("grepContexts.code"),
        textField("grepContexts.title"),
        getTaxonomyContextMapping("context"),
        getTaxonomyContextMapping("contexts"),
        ObjectField(
          "subjectPage",
          properties = List(
            keywordField("id"),
            keywordField("name"),
            ObjectField("domainObject", enabled = Some(false))
          ) ++
            languageValuesMapping("aboutTitle") ++
            languageValuesMapping("aboutDescription") ++
            languageValuesMapping("metaDescription")
        )
      )

      val dynamics =
        languageValuesMapping("title") ++
          languageValuesMapping("content")

      properties(fields ++ dynamics)
    }

    def indexDocuments(numShards: Option[Int], indexingBundle: IndexingBundle): Try[ReindexResult] = {
      indexDocumentsInBulk(numShards) { indexName =>
        sendToElastic(indexingBundle, indexName)
      }
    }

    def indexDocuments(numShards: Option[Int]): Try[ReindexResult] = for {
      grepBundle     <- grepApiClient.getGrepBundle()
      taxonomyBundle <- taxonomyApiClient.getTaxonomyBundle(true)
      indexingBundle = IndexingBundle(grepBundle.some, taxonomyBundle.some, None)
      result <- indexDocuments(numShards, indexingBundle)
    } yield result

    private def getFrontPage(contentUri: Option[String]): Try[Option[SubjectPage]] = {
      contentUri.map(ContentURIUtil.parseFrontpageId) match {
        case Some(Success(frontpageId)) =>
          val subjectPage = frontpageApiClient.getSubjectPage(frontpageId)
          subjectPage match {
            case Failure(exception: HttpRequestException) if exception.is404 =>
              Success(None)
            case Failure(ex) =>
              Failure(ex)
            case Success(value) =>
              Success(Some(value))
          }
        case _ => Success(None)
      }
    }

    def createIndexRequest(indexingBundle: IndexingBundle, node: Node, indexName: String): Try[IndexRequest] = {
      for {
        frontpage  <- getFrontPage(node.contentUri)
        searchable <- searchConverterService.asSearchableNode(node, frontpage, indexingBundle)
        source = CirceUtil.toJsonString(searchable)
      } yield indexInto(indexName).doc(source).id(node.id)
    }

    def sendChunkToElastic(
        indexingBundle: IndexingBundle,
        chunk: List[Node],
        indexName: String
    ): Try[BulkIndexResult] = {
      chunk
        .traverse(node => createIndexRequest(indexingBundle, node, indexName))
        .map(executeRequests)
        .flatten
    }

    def sendToElastic(indexingBundle: IndexingBundle, indexName: String): Try[BulkIndexResult] = {
      val taxBundle = indexingBundle.taxonomyBundle match {
        case None        => taxonomyApiClient.getTaxonomyBundle(true).?
        case Some(value) => value
      }

      taxBundle.nodes
        .grouped(props.IndexBulkSize)
        .toList
        .traverse(group => sendChunkToElastic(indexingBundle, group, indexName))
        .map(countBulkIndexed)
    }
  }
}
