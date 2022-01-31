/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties.{AudioTagSearchDocument, AudioTagSearchIndex}
import no.ndla.audioapi.model.domain.{AudioMetaInformation, SearchableTag}
import no.ndla.audioapi.repository.{AudioRepository, Repository}
import org.json4s.native.Serialization.write

import scala.util.{Success, Try}

trait TagIndexService {
  this: SearchConverterService with IndexService with AudioRepository =>
  val tagIndexService: TagIndexService

  class TagIndexService extends LazyLogging with IndexService[AudioMetaInformation, SearchableTag] {
    override val documentType: String = AudioTagSearchDocument
    override val searchIndex: String = AudioTagSearchIndex
    override val repository: Repository[AudioMetaInformation] = audioRepository

    override def createIndexRequests(domainModel: AudioMetaInformation, indexName: String): Try[Seq[IndexRequest]] = {
      val tags = searchConverterService.asSearchableTags(domainModel)

      Success(
        tags.map(t => {
          val source = write(t)
          indexInto(indexName).doc(source).id(s"${t.language}.${t.tag}")
        })
      )
    }

    def getMapping: MappingDefinition = {
      properties(
        List(
          textField("tag"),
          keywordField("language")
        )
      )
    }
  }

}
