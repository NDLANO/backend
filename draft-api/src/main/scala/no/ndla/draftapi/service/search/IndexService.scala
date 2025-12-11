/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.ElasticField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.typesafe.scalalogging.StrictLogging
import no.ndla.draftapi.DraftApiProperties
import no.ndla.database.DBUtility
import no.ndla.draftapi.repository.Repository
import no.ndla.search.model.domain.{BulkIndexResult, ReindexResult}
import no.ndla.search.{BaseIndexService, SearchLanguage}
import scalikejdbc.DBSession

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait IndexService[D, T](using props: DraftApiProperties, searchLanguage: SearchLanguage, dbUtility: DBUtility)
    extends BaseIndexService
    with StrictLogging {
  override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow
  val repository: Repository[D]

  def indexAsync(id: Long, doc: D)(implicit ec: ExecutionContext): Future[Try[D]] = {
    val fut = Future {
      indexDocument(doc)
    }
    val logIndexFailure = (id: Long, ex: Throwable) => logger.error(s"Failed to index into $searchIndex, id: $id", ex)

    fut.onComplete {
      case Success(Success(_))  => logger.info(s"Successfully indexed into $searchIndex, id: $id")
      case Success(Failure(ex)) => logIndexFailure(id, ex)
      case Failure(ex)          => logIndexFailure(id, ex)
    }

    fut
  }

  def createIndexRequests(domainModel: D, indexName: String): Seq[IndexRequest]

  def indexDocument(imported: D): Try[D] = {
    for {
      _       <- createIndexIfNotExists()
      requests = createIndexRequests(imported, searchIndex)
      _       <- executeRequests(requests)
    } yield imported
  }

  def indexDocuments(numShards: Option[Int]): Try[ReindexResult] = synchronized {
    indexDocumentsInBulk(numShards)(sendToElastic)
  }

  def sendToElastic(indexName: String): Try[BulkIndexResult] = dbUtility.tryReadOnly { implicit session =>
    getRanges
      .flatMap(ranges => {
        ranges.traverse { case (start, end) =>
          repository.documentsWithIdBetween(start, end).flatMap(toIndex => indexDocuments(toIndex, indexName))
        }
      })
      .map(countBulkIndexed)
  }

  def getRanges(using DBSession): Try[List[(Long, Long)]] = {
    repository
      .minMaxId
      .map { case (minId, maxId) =>
        Seq.range(minId, maxId + 1).grouped(props.IndexBulkSize).map(group => (group.head, group.last)).toList
      }
  }

  def indexDocuments(contents: Seq[D], indexName: String): Try[BulkIndexResult] = {
    if (contents.isEmpty) {
      Success(BulkIndexResult.empty)
    } else {
      val requests = contents.flatMap(content => {
        createIndexRequests(content, indexName)
      })

      executeRequests(requests) match {
        case Success(result) =>
          logger.info(s"Indexed ${result.successful} documents ($searchIndex). No of failed items: ${result.failed}")
          Success(result)
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
      searchLanguage
        .languageAnalyzers
        .map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
            .fielddata(false)
            .analyzer(langAnalyzer.analyzer)
            .fields(keywordField("raw"))
        )
    } else {
      searchLanguage
        .languageAnalyzers
        .map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString}").fielddata(false).analyzer(langAnalyzer.analyzer)
        )
    }
  }
}
