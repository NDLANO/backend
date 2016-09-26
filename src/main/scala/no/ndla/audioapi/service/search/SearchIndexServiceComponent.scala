/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.audioapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.repository.AudioRepositoryComponent

trait SearchIndexServiceComponent {
  this: AudioRepositoryComponent with ElasticContentIndexComponent =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {

    def indexDocuments() = {
      synchronized {
        val start = System.currentTimeMillis

        val newIndexName = elasticContentIndex.createIndex()
        val oldIndexName = elasticContentIndex.aliasTarget

        oldIndexName match {
          case None => elasticContentIndex.updateAliasTarget(oldIndexName, newIndexName)
          case Some(_) =>
        }

        var numIndexed = 0
        audioRepository.applyToAll(docs => {
          numIndexed += elasticContentIndex.indexDocuments(docs, newIndexName)
        })

        oldIndexName.foreach(indexName => {
          elasticContentIndex.updateAliasTarget(oldIndexName, newIndexName)
          elasticContentIndex.delete(indexName)
        })

        val result = s"Completed indexing of $numIndexed documents in ${System.currentTimeMillis() - start} ms."
        logger.info(result)
        result
      }
    }
  }

}
