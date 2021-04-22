/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.Elastic4sClient
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.search.SearchableAudioInformation
import no.ndla.audioapi.repository.AudioRepository
import org.json4s.native.Serialization.write

import scala.util.{Success, Try}

trait AudioIndexService {
  this: Elastic4sClient with SearchConverterService with IndexService with AudioRepository =>

  val audioIndexService: AudioIndexService

  class AudioIndexService extends LazyLogging with IndexService[AudioMetaInformation, SearchableAudioInformation] {
    override val documentType: String = AudioApiProperties.SearchDocument
    override val searchIndex: String = AudioApiProperties.SearchIndex
    override val repository: AudioRepository = audioRepository

    override def createIndexRequests(domainModel: AudioMetaInformation, indexName: String): Try[Seq[IndexRequest]] = {
      val source = write(searchConverterService.asSearchableAudioInformation(domainModel))
      Success(Seq(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))) // TODO: Maybe actually utilize try here?
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        List(
          intField("id"),
          keywordField("license"),
          keywordField("defaultTitle"),
          textField("authors").fielddata(true),
          keywordField("audioType")
        ) ++
          generateLanguageSupportedFieldList("titles", keepRaw = true) ++
          generateLanguageSupportedFieldList("tags")
      )
    }
  }

}
