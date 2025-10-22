/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.concept.ConceptStatus
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.api.ResultWindowTooLargeException
import no.ndla.conceptapi.model.domain.SearchResult
import no.ndla.conceptapi.model.search.DraftSearchSettings
import no.ndla.language.Language.AllLanguages
import no.ndla.search.AggregationBuilder.{buildTermsAggregation, getAggregationsFromResult}
import no.ndla.search.NdlaE4sClient

import java.util.concurrent.Executors
import scala.concurrent.*
import scala.util.{Failure, Success, Try}

class DraftConceptSearchService(using
    e4sClient: NdlaE4sClient,
    draftConceptIndexService: DraftConceptIndexService,
    searchConverterService: SearchConverterService,
    props: Props,
) extends StrictLogging
    with SearchService[api.ConceptSummaryDTO] {
  override val searchIndex: String = props.DraftConceptSearchIndex

  override def hitToApiModel(hitString: String, language: String): api.ConceptSummaryDTO = searchConverterService
    .hitAsConceptSummary(hitString, language)

  def all(settings: DraftSearchSettings): Try[SearchResult[api.ConceptSummaryDTO]] =
    executeSearch(boolQuery(), settings)

  def matchingQuery(query: String, settings: DraftSearchSettings): Try[SearchResult[api.ConceptSummaryDTO]] = {
    val language =
      if (settings.searchLanguage == AllLanguages || settings.fallback) "*"
      else settings.searchLanguage

    val fullQuery = boolQuery().must(
      boolQuery().should(
        List(
          simpleStringQuery(query).field(s"title.$language", 2),
          simpleStringQuery(query).field(s"content.$language", 1),
          simpleStringQuery(query).field(s"tags.$language", 1),
          simpleStringQuery(query).field(s"gloss", 1),
          idsQuery(query),
        ) ++
          buildNestedEmbedField(List(query), None, settings.searchLanguage, settings.fallback) ++
          buildNestedEmbedField(List.empty, Some(query), settings.searchLanguage, settings.fallback)
      )
    )

    executeSearch(fullQuery, settings)
  }

  def executeSearch(
      queryBuilder: BoolQuery,
      settings: DraftSearchSettings,
  ): Try[SearchResult[api.ConceptSummaryDTO]] = {
    val idFilter =
      if (settings.withIdIn.isEmpty) None
      else Some(idsQuery(settings.withIdIn))
    val typeFilter          = settings.conceptType.map(ct => termsQuery("conceptType", ct))
    val statusFilter        = boolStatusFilter(settings.statusFilter)
    val tagFilter           = languageOrFilter(settings.tagsToFilterBy, "tags", settings.searchLanguage, settings.fallback)
    val userFilter          = orFilter(settings.userFilter, "updatedBy")
    val responsibleIdFilter = Option.when(settings.responsibleIdFilter.nonEmpty) {
      termsQuery("responsible.responsibleId", settings.responsibleIdFilter)
    }

    val (languageFilter, searchLanguage) = settings.searchLanguage match {
      case "" | AllLanguages      => (None, "*")
      case _ if settings.fallback => (None, "*")
      case lang                   => (Some(existsQuery(s"title.$lang")), lang)
    }

    val embedResourceAndIdFilter =
      buildNestedEmbedField(settings.embedResource, settings.embedId, settings.searchLanguage, settings.fallback)

    val filters = List(
      idFilter,
      typeFilter,
      languageFilter,
      tagFilter,
      statusFilter,
      userFilter,
      embedResourceAndIdFilter,
      responsibleIdFilter,
    )

    val filteredSearch = queryBuilder.filter(filters.flatten)

    val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
    val requestedResultWindow = settings.pageSize * settings.page
    if (requestedResultWindow > props.ElasticSearchIndexMaxResultWindow) {
      logger.info(
        s"Max supported results are ${props.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow"
      )
      Failure(ResultWindowTooLargeException.default)
    } else {
      val aggregations    = buildTermsAggregation(settings.aggregatePaths, List(draftConceptIndexService.getMapping))
      val searchToExecute = search(searchIndex)
        .size(numResults)
        .from(startAt)
        .trackTotalHits(true)
        .query(filteredSearch)
        .highlighting(highlight("*"))
        .aggs(aggregations)
        .sortBy(getSortDefinition(settings.sort, searchLanguage))

      val searchWithScroll =
        if (startAt == 0 && settings.shouldScroll) {
          searchToExecute.scroll(props.ElasticSearchScrollKeepAlive)
        } else {
          searchToExecute
        }

      e4sClient.execute(searchWithScroll) match {
        case Success(response) => Success(
            SearchResult(
              totalCount = response.result.totalHits,
              page = Some(settings.page),
              pageSize = numResults,
              language = searchLanguage,
              results = getHits(response.result, settings.searchLanguage),
              aggregations = getAggregationsFromResult(response.result),
              scrollId = response.result.scrollId,
            )
          )
        case Failure(ex) => errorHandler(ex)
      }
    }
  }

  private def boolStatusFilter(statuses: Set[String]): Some[BoolQuery] = {
    if (statuses.isEmpty) {
      Some(boolQuery().not(termQuery("status.current", ConceptStatus.ARCHIVED.toString)))
    } else {
      val draftStatuses = Seq("status.current", "status.other")
      Some(boolQuery().should(draftStatuses.flatMap(ds => statuses.map(s => termQuery(ds, s)))))
    }
  }

  override def scheduleIndexDocuments(): Unit = {
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    val f = Future {
      draftConceptIndexService.indexDocuments(None)
    }

    f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
    f.foreach {
      case Success(reindexResult) =>
        logger.info(s"Completed indexing of ${reindexResult.totalIndexed} concepts in ${reindexResult.millisUsed} ms.")
      case Failure(ex) => logger.warn(ex.getMessage, ex)
    }
  }

}
