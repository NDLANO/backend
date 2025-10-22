/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.ElasticField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.repository.Repository
import no.ndla.search.model.domain.{BulkIndexResult, ReindexResult}
import no.ndla.search.{BaseIndexService, NdlaE4sClient, SearchLanguage}

import scala.util.{Failure, Success, Try}

abstract class IndexService[D, T](using e4sClient: NdlaE4sClient, props: Props, searchLanguage: SearchLanguage)
    extends BaseIndexService
    with StrictLogging {
  override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow

  val documentType: String
  val searchIndex: String
  val repository: Repository[D]

  def getMapping: MappingDefinition
  def createIndexRequests(domainModel: D, indexName: String): Try[Seq[IndexRequest]]

  def indexDocument(imported: D): Try[D] = {
    for {
      _        <- createIndexIfNotExists()
      requests <- createIndexRequests(imported, searchIndex)
      _        <- executeRequests(requests)
    } yield imported
  }

  def indexDocuments(numShards: Option[Int]): Try[ReindexResult] = synchronized {
    indexDocumentsInBulk(numShards)(sendToElastic)
  }

  def sendToElastic(indexName: String): Try[BulkIndexResult] = {
    getRanges.flatMap(ranges => {
      ranges
        .traverse { case (start, end) =>
          repository.documentsWithIdBetween(start, end).flatMap(toIndex => indexDocuments(toIndex, indexName))
        }
        .map(countBulkIndexed)
    })
  }

  def getRanges: Try[List[(Long, Long)]] = {
    val minMaxT = repository.minMaxId
    minMaxT.flatMap { case (minId, maxId) =>
      Try {
        Seq.range(minId, maxId + 1).grouped(props.IndexBulkSize).map(group => (group.head, group.last)).toList
      }
    }

  }

  def indexDocuments(contents: Seq[D], indexName: String): Try[BulkIndexResult] = {
    if (contents.isEmpty) {
      Success(BulkIndexResult.empty)
    } else {
      val requests = contents.traverse(content => createIndexRequests(content, indexName))
      requests.flatMap(rs => {
        executeRequests(rs.flatten) match {
          case Success(result) =>
            logger.info(s"Indexed ${result.successful} documents ($searchIndex). No of failed items: ${result.failed}")
            Success(result)
          case Failure(ex) => Failure(ex)
        }
      })

    }
  }

  /** @deprecated
    *   Returns Sequence of FieldDefinitions for a given field.
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
      searchLanguage
        .languageAnalyzers
        .map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString()}")
            .analyzer(langAnalyzer.analyzer)
            .fields(keywordField("raw"))
        )
    } else {
      searchLanguage
        .languageAnalyzers
        .map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString()}").analyzer(langAnalyzer.analyzer)
        )
    }
  }
}
