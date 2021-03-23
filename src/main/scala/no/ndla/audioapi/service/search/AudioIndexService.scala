/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.{MappingDefinition, NestedField}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.Elastic4sClient
import no.ndla.audioapi.model.Language._
import no.ndla.audioapi.model.domain.{AudioMetaInformation, ReindexResult}
import no.ndla.audioapi.model.search.{SearchableAudioInformation, SearchableLanguageFormats}
import no.ndla.audioapi.repository.AudioRepository
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait AudioIndexService {
  this: Elastic4sClient with SearchConverterService with IndexService with AudioRepository =>

  val audioIndexService: AudioIndexService

  class AudioIndexService extends LazyLogging with IndexService[AudioMetaInformation, SearchableAudioInformation] {
    override val documentType: String = AudioApiProperties.SearchDocument
    override val searchIndex: String = AudioApiProperties.SearchIndex
    override val repository: AudioRepository = audioRepository

    override def createIndexRequests(domainModel: AudioMetaInformation, indexName: String): Seq[IndexRequest] = {
      val source = write(searchConverterService.asSearchableAudioInformation(domainModel))
      Seq(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))
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
          generateLanguageSupportedFieldList("tags", keepRaw = false)
      )
    }
  }

}
