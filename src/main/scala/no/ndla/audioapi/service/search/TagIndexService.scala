/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties.{AudioTagSearchDocument, AudioTagSearchIndex}
import no.ndla.audioapi.model.domain.{AudioMetaInformation, SearchableTag}
import no.ndla.audioapi.model.search.SearchableLanguageFormats
import no.ndla.audioapi.repository.{AudioRepository, Repository}
import org.json4s.native.Serialization.write

trait TagIndexService {
  this: SearchConverterService with IndexService with AudioRepository =>
  val tagIndexService: TagIndexService

  class TagIndexService extends LazyLogging with IndexService[AudioMetaInformation, SearchableTag] {
    override val documentType: String = AudioTagSearchDocument
    override val searchIndex: String = AudioTagSearchIndex
    override val repository: Repository[AudioMetaInformation] = audioRepository

    override def createIndexRequests(domainModel: AudioMetaInformation, indexName: String): Seq[IndexRequest] = {
      val tags = searchConverterService.asSearchableTags(domainModel)

      tags.map(t => {
        val source = write(t)
        indexInto(indexName / documentType).doc(source).id(s"${t.language}.${t.tag}")
      })
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        List(
          textField("tag"),
          keywordField("language")
        )
      )
    }
  }

}
