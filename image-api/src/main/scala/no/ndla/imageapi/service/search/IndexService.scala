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
import no.ndla.imageapi.repository.{ImageRepository, Repository}
import no.ndla.search.model.domain.{BulkIndexResult, ReindexResult}
import no.ndla.search.{BaseIndexService, Elastic4sClient}

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient & ImageRepository & BaseIndexService & Props =>

  trait IndexService[D, T <: AnyRef] extends BaseIndexService with StrictLogging {
    override val MaxResultWindowOption: Int = props.ElasticSearchIndexMaxResultWindow
    val repository: Repository[D]

    def createIndexRequests(domainModel: D, indexName: String): Seq[IndexRequest]

    def indexDocument(imported: D): Try[D] = {
      for {
        _ <- createIndexIfNotExists()
        requests = createIndexRequests(imported, searchIndex)
        _ <- executeRequests(requests)
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
        Seq
          .range(minId, maxId + 1)
          .grouped(props.IndexBulkSize)
          .map(group => (group.head, group.last))
          .toList
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
  }
}
