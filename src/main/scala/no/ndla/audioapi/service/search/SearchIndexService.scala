/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.audioapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.model.domain.{AudioMetaInformation, ReindexResult}
import no.ndla.audioapi.repository.AudioRepository

import scala.util.{Failure, Success, Try}

trait SearchIndexService {
  this: AudioRepository with ElasticIndexService =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {

    def indexDocument(imported: AudioMetaInformation): Try[AudioMetaInformation] = {
      for {
        _ <- elasticIndexService.aliasTarget.map {
          case Some(index) => Success(index)
          case None => elasticIndexService.createIndex().map(newIndex => elasticIndexService.updateAliasTarget(None, newIndex))
        }
        imported <- elasticIndexService.indexDocument(imported)
      } yield imported
    }

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        elasticIndexService.createIndex().flatMap(indexName => {
          val operations = for {
            numIndexed <- sendToElastic(indexName)
            aliasTarget <- elasticIndexService.aliasTarget
            updatedTarget <- elasticIndexService.updateAliasTarget(aliasTarget, indexName)
            deleted <- elasticIndexService.delete(aliasTarget)
          } yield numIndexed

          operations match {
            case Failure(f) => {
              elasticIndexService.delete(Some(indexName))
              Failure(f)
            }
            case Success(totalIndexed) => {
              Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
            }
          }
        })
      }
    }

    def sendToElastic(indexName: String): Try[Int] = {
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val numberInBulk = elasticIndexService.indexDocuments(audioRepository.audiosWithIdBetween(range._1, range._2), indexName)
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f) => return Failure(f)
          }
        })
        numIndexed
      })
    }

    def getRanges:Try[List[(Long,Long)]] = {
      Try{
        val (minId, maxId) = audioRepository.minMaxId
        Seq.range(minId, maxId).grouped(AudioApiProperties.IndexBulkSize).map(group => (group.head, group.last + 1)).toList
      }
    }
  }
}
