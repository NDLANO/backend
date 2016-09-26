/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.audioapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.repository.AudioRepository

trait SearchIndexService {
  this: AudioRepository with ElasticIndexService =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {

    def indexDocuments() = {
      synchronized {
        val start = System.currentTimeMillis

        val newIndexName = elasticIndexService.createIndex()
        val oldIndexName = elasticIndexService.aliasTarget

        oldIndexName match {
          case None => elasticIndexService.updateAliasTarget(oldIndexName, newIndexName)
          case Some(_) =>
        }

        var numIndexed = 0
        audioRepository.applyToAll(docs => {
          numIndexed += elasticIndexService.indexDocuments(docs, newIndexName)
        })

        oldIndexName.foreach(indexName => {
          elasticIndexService.updateAliasTarget(oldIndexName, newIndexName)
          elasticIndexService.delete(indexName)
        })

        val result = s"Completed indexing of $numIndexed documents in ${System.currentTimeMillis() - start} ms."
        logger.info(result)
        result
      }
    }
  }

}
