/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.domain.{AudioMetaInformation, SearchableTag}
import no.ndla.audioapi.repository.{AudioRepository, Repository}
import no.ndla.common.CirceUtil

import scala.util.{Success, Try}

trait TagIndexService {
  this: SearchConverterService with IndexService with AudioRepository with Props =>
  val tagIndexService: TagIndexService

  class TagIndexService extends StrictLogging with IndexService[AudioMetaInformation, SearchableTag] {
    import props._

    override val documentType: String                         = AudioTagSearchDocument
    override val searchIndex: String                          = AudioTagSearchIndex
    override val repository: Repository[AudioMetaInformation] = audioRepository

    override def createIndexRequests(domainModel: AudioMetaInformation, indexName: String): Try[Seq[IndexRequest]] = {
      val tags = searchConverterService.asSearchableTags(domainModel)

      Success(
        tags.map(t => {
          val source = CirceUtil.toJsonString(t)
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
