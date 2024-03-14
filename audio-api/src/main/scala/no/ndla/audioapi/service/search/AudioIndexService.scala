/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.{ElasticField, ObjectField}
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.MissingIdException
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.search.SearchableAudioInformation
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.common.CirceUtil
import no.ndla.search.Elastic4sClient

import scala.util.{Failure, Try}

trait AudioIndexService {
  this: Elastic4sClient
    with SearchConverterService
    with IndexService
    with SeriesIndexService
    with AudioRepository
    with Props =>

  val audioIndexService: AudioIndexService

  class AudioIndexService extends StrictLogging with IndexService[AudioMetaInformation, SearchableAudioInformation] {
    import props._
    override val documentType: String        = SearchDocument
    override val searchIndex: String         = SearchIndex
    override val repository: AudioRepository = audioRepository

    override def createIndexRequests(domainModel: AudioMetaInformation, indexName: String): Try[Seq[IndexRequest]] = {
      domainModel.id match {
        case None =>
          Failure(MissingIdException(s"Missing id when creating index request for $indexName. This is a bug."))
        case Some(domainId) =>
          searchConverterService
            .asSearchableAudioInformation(domainModel)
            .map(sai => {
              val source = CirceUtil.toJsonString(sai)
              Seq(indexInto(indexName).doc(source).id(domainId.toString))
            })
      }
    }

    def getMapping: MappingDefinition = {
      val fields: Seq[ElasticField] = List(
        intField("id"),
        keywordField("license"),
        keywordField("defaultTitle"),
        textField("authors").fielddata(true),
        keywordField("audioType"),
        nestedField("series").fields(seriesIndexService.seriesIndexFields),
        nestedField("podcastMeta").fields(
          keywordField("language"),
          ObjectField(
            "coverPhoto",
            properties = Seq(
              keywordField("imageId"),
              keywordField("altText")
            )
          )
        )
      )

      val dynamics: Seq[DynamicTemplateRequest] =
        generateLanguageSupportedDynamicTemplates("titles", keepRaw = true) ++
          generateLanguageSupportedDynamicTemplates("tags") ++
          generateLanguageSupportedDynamicTemplates("manuscript") ++
          generateLanguageSupportedDynamicTemplates("filePaths") ++
          generateLanguageSupportedDynamicTemplates("podcastMetaIntroduction")

      properties(fields).dynamicTemplates(dynamics)
    }
  }

}
