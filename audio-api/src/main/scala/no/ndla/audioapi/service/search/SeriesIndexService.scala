/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.fields.ElasticField
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.domain.Series
import no.ndla.audioapi.model.search.SearchableSeries
import no.ndla.audioapi.repository.SeriesRepository
import no.ndla.common.CirceUtil
import no.ndla.database.DBUtility
import no.ndla.search.{NdlaE4sClient, SearchLanguage}

import scala.util.{Failure, Success, Try}

class SeriesIndexService(using
    elastic4sClient: NdlaE4sClient,
    searchConverterService: SearchConverterService,
    seriesRepository: SeriesRepository,
    props: Props,
    searchLanguage: SearchLanguage,
    dbUtility: DBUtility,
) extends IndexService[Series, SearchableSeries]
    with StrictLogging {
  override val documentType: String         = props.SeriesSearchDocument
  override val searchIndex: String          = props.SeriesSearchIndex
  override val repository: SeriesRepository = seriesRepository

  override def createIndexRequests(domainModel: Series, indexName: String): Try[Seq[IndexRequest]] = {
    searchConverterService.asSearchableSeries(domainModel) match {
      case Failure(exception)  => Failure(exception)
      case Success(searchable) =>
        val source = CirceUtil.toJsonString(searchable)
        Success(Seq(indexInto(indexName).doc(source).id(domainModel.id.toString)))
    }
  }

  val seriesIndexFields: Seq[ElasticField] =
    List(intField("id"), keywordField("defaultTitle"), dateField("lastUpdated"))

  val seriesDynamics = generateLanguageSupportedFieldList("titles", keepRaw = true) ++
    generateLanguageSupportedFieldList("descriptions", keepRaw = true)

  def getMapping: MappingDefinition = properties(seriesIndexFields ++ seriesDynamics)
}
