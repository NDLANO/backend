/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicTemplateRequest
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.Elastic4sClient
import no.ndla.audioapi.model.api.MissingIdException
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.search.SearchableAudioInformation
import no.ndla.audioapi.repository.AudioRepository
import org.json4s.native.Serialization.write

import scala.util.{Failure, Try}

trait AudioIndexService {
  this: Elastic4sClient with SearchConverterService with IndexService with SeriesIndexService with AudioRepository =>

  val audioIndexService: AudioIndexService

  class AudioIndexService extends LazyLogging with IndexService[AudioMetaInformation, SearchableAudioInformation] {
    override val documentType: String = AudioApiProperties.SearchDocument
    override val searchIndex: String = AudioApiProperties.SearchIndex
    override val repository: AudioRepository = audioRepository

    override def createIndexRequests(domainModel: AudioMetaInformation, indexName: String): Try[Seq[IndexRequest]] = {
      domainModel.id match {
        case None =>
          Failure(MissingIdException(s"Missing id when creating index request for $indexName. This is a bug."))
        case Some(domainId) =>
          searchConverterService
            .asSearchableAudioInformation(domainModel)
            .map(sai => {
              val source = write(sai)
              Seq(indexInto(indexName / documentType).doc(source).id(domainId.toString))
            })
      }
    }

    def getMapping: MappingDefinition = {
      val fields: Seq[FieldDefinition] = List(
        intField("id"),
        keywordField("license"),
        keywordField("defaultTitle"),
        textField("authors").fielddata(true),
        keywordField("audioType"),
        nestedField("series").fields(seriesIndexService.seriesIndexFields),
        nestedField("podcastMeta").fields(
          keywordField("language"),
          objectField("coverPhoto").fields(
            keywordField("imageId"),
            keywordField("altText")
          )
        )
      )

      val dynamics: Seq[DynamicTemplateRequest] =
        generateLanguageSupportedDynamicTemplates("titles", keepRaw = true) ++
          generateLanguageSupportedDynamicTemplates("tags") ++
          generateLanguageSupportedDynamicTemplates("manuscript") ++
          generateLanguageSupportedDynamicTemplates("podcastMetaIntroduction")

      mapping(documentType).fields(fields).dynamicTemplates(dynamics)
    }
  }

}
