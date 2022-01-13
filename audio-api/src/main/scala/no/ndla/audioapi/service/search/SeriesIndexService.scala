/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.fields.ElasticField
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.model.domain.Series
import no.ndla.audioapi.model.search.SearchableSeries
import no.ndla.audioapi.repository.SeriesRepository
import no.ndla.search.Elastic4sClient
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
          Success(Seq(indexInto(indexName).doc(source).id(domainModel.id.toString)))
      }
    }

    val seriesIndexFields: Seq[ElasticField] =
      List(
        intField("id"),
        keywordField("defaultTitle"),
        dateField("lastUpdated"),
      )

    val seriesDynamics: Seq[DynamicTemplateRequest] =
      generateLanguageSupportedDynamicTemplates("titles", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("descriptions", keepRaw = true)

    def getMapping: MappingDefinition = properties(seriesIndexFields).dynamicTemplates(seriesDynamics)
  }

}
