/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.typesafe.scalalogging.StrictLogging
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.repository.Repository
import no.ndla.search.model.domain.{BulkIndexResult, ReindexResult}
import no.ndla.search.{BaseIndexService, NdlaE4sClient, SearchLanguage}

import scala.util.{Failure, Success, Try}

abstract class IndexService(using e4sClient: NdlaE4sClient, searchLanguage: SearchLanguage, props: Props)
    extends BaseIndexService
    with StrictLogging {
  override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow
  val repository: Repository[ImageMetaInformation]

  def createIndexRequests(domainModel: ImageMetaInformation, indexName: String): Seq[IndexRequest]

  def indexDocument(imported: ImageMetaInformation): Try[ImageMetaInformation] = {
    for {
      _       <- createIndexIfNotExists()
      requests = createIndexRequests(imported, searchIndex)
      _       <- executeRequests(requests)
    } yield imported
  }

  def indexDocuments(numShards: Option[Int]): Try[ReindexResult] = synchronized {
    indexDocumentsInBulk(numShards)(sendToElastic)
  }

  def sendToElastic(indexName: String): Try[BulkIndexResult] = {
    getRanges
      .flatMap(ranges => {
        ranges.traverse { case (start, end) =>
          val toIndex = repository.documentsWithIdBetween(start, end)
          indexDocuments(toIndex, indexName)
        }
      })
      .map(countBulkIndexed)
  }

  def getRanges: Try[List[(Long, Long)]] = {
    Try {
      val (minId, maxId) = repository.minMaxId
      Seq.range(minId, maxId + 1).grouped(props.IndexBulkSize).map(group => (group.head, group.last)).toList
    }
  }

  def indexDocuments(contents: Seq[ImageMetaInformation], indexName: String): Try[BulkIndexResult] = {
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
}
