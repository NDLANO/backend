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
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicTemplateRequest
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.Elastic4sClient
import no.ndla.audioapi.model.domain.Series
import no.ndla.audioapi.model.search.SearchableSeries
import no.ndla.audioapi.repository.SeriesRepository
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait SeriesIndexService {
  this: Elastic4sClient with SearchConverterService with IndexService with SeriesRepository =>

  val seriesIndexService: SeriesIndexService

  class SeriesIndexService extends LazyLogging with IndexService[Series, SearchableSeries] {
    override val documentType: String = AudioApiProperties.SeriesSearchDocument
    override val searchIndex: String = AudioApiProperties.SeriesSearchIndex
    override val repository: SeriesRepository = seriesRepository

    override def createIndexRequests(domainModel: Series, indexName: String): Try[Seq[IndexRequest]] = {
      searchConverterService.asSearchableSeries(domainModel) match {
        case Failure(exception) => Failure(exception)
        case Success(searchable) =>
          val source = write(searchable)
          Success(Seq(indexInto(indexName / documentType).doc(source).id(domainModel.id.toString)))
      }
    }

    val seriesIndexFields: Seq[FieldDefinition] =
      List(
        intField("id"),
        keywordField("defaultTitle"),
        dateField("lastUpdated"),
      )

    val seriesDynamics: Seq[DynamicTemplateRequest] =
      generateLanguageSupportedDynamicTemplates("titles", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("descriptions", keepRaw = true)

    def getMapping: MappingDefinition = mapping(documentType).fields(seriesIndexFields).dynamicTemplates(seriesDynamics)
  }

}
