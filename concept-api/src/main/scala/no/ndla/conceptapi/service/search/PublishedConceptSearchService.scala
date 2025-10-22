/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.SimpleQueryStringFlag
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.typesafe.scalalogging.StrictLogging
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.api.ResultWindowTooLargeException
import no.ndla.conceptapi.model.domain.SearchResult
import no.ndla.conceptapi.model.search.SearchSettings
import no.ndla.language.Language.AllLanguages
import no.ndla.search.AggregationBuilder.{buildTermsAggregation, getAggregationsFromResult}
import no.ndla.search.NdlaE4sClient

import java.util.concurrent.Executors
import scala.concurrent.*
import scala.util.{Failure, Success, Try}

class PublishedConceptSearchService(using
    e4sClient: NdlaE4sClient,
    publishedConceptIndexService: PublishedConceptIndexService,
    searchConverterService: SearchConverterService,
    props: Props,
) extends StrictLogging
    with SearchService[api.ConceptSummaryDTO] {
  override val searchIndex: String = props.PublishedConceptSearchIndex

  override def hitToApiModel(hitString: String, language: String): api.ConceptSummaryDTO = searchConverterService
    .hitAsConceptSummary(hitString, language)

  def all(settings: SearchSettings): Try[SearchResult[api.ConceptSummaryDTO]] = executeSearch(boolQuery(), settings)

  def matchingQuery(query: String, settings: SearchSettings): Try[SearchResult[api.ConceptSummaryDTO]] = {
    val language =
      if (settings.fallback) "*"
      else settings.searchLanguage

    val fullQuery =
      if (settings.exactTitleMatch) {
        boolQuery().must(simpleStringQuery(query).flags(SimpleQueryStringFlag.NONE).field(s"title.$language.lower"))
      } else {
        boolQuery().must(
          boolQuery().should(
            List(
              simpleStringQuery(query).field(s"title.$language", 2),
              simpleStringQuery(query).field(s"content.$language", 1),
              simpleStringQuery(query).field(s"gloss", 1),
              idsQuery(query),
            ) ++
              buildNestedEmbedField(List(query), None, settings.searchLanguage, settings.fallback) ++
              buildNestedEmbedField(List.empty, Some(query), settings.searchLanguage, settings.fallback)
          )
        )
      }
    executeSearch(fullQuery, settings)
  }

  def executeSearch(queryBuilder: BoolQuery, settings: SearchSettings): Try[SearchResult[api.ConceptSummaryDTO]] = {
    val idFilter =
      if (settings.withIdIn.isEmpty) None
      else Some(idsQuery(settings.withIdIn))
    val typeFilter = settings.conceptType.map(ct => termsQuery("conceptType", ct))
    val tagFilter  = languageOrFilter(settings.tagsToFilterBy, "tags", settings.searchLanguage, settings.fallback)

    val (languageFilter, searchLanguage) = settings.searchLanguage match {
      case "" | AllLanguages => (None, "*")
      case lang              =>
        if (settings.fallback) (None, "*")
        else (Some(existsQuery(s"title.$lang")), lang)
    }

    val embedResourceAndIdFilter =
      buildNestedEmbedField(settings.embedResource, settings.embedId, settings.searchLanguage, settings.fallback)

    val filters = List(idFilter, typeFilter, languageFilter, tagFilter, embedResourceAndIdFilter)

    val filteredSearch = queryBuilder.filter(filters.flatten)

    val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
    val requestedResultWindow = settings.pageSize * settings.page
    if (requestedResultWindow > props.ElasticSearchIndexMaxResultWindow) {
      logger.info(
        s"Max supported results are ${props.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow"
      )
      Failure(ResultWindowTooLargeException.default)
    } else {
      val aggregations    = buildTermsAggregation(settings.aggregatePaths, List(publishedConceptIndexService.getMapping))
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
        case Success(response) =>
          val aggResult = getAggregationsFromResult(response.result)
          Success(
            SearchResult(
              totalCount = response.result.totalHits,
              page = Some(settings.page),
              pageSize = numResults,
              language = searchLanguage,
              results = getHits(response.result, settings.searchLanguage),
              aggregations = aggResult,
              scrollId = response.result.scrollId,
            )
          )
        case Failure(ex) => errorHandler(ex)
      }
    }
  }

  override def scheduleIndexDocuments(): Unit = {
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    val f = Future {
      publishedConceptIndexService.indexDocuments(None)
    }

    f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
    f.foreach {
      case Success(reindexResult) =>
        logger.info(s"Completed indexing of ${reindexResult.totalIndexed} concepts in ${reindexResult.millisUsed} ms.")
      case Failure(ex) => logger.warn(ex.getMessage, ex)
    }
  }

}
