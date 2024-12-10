/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.ErrorHandling
import no.ndla.audioapi.model.domain.SeriesSearchSettings
import no.ndla.audioapi.model.search.SearchableSeries
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.service.ConverterService
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.*
import no.ndla.language.Language.AllLanguages
import no.ndla.search.Elastic4sClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait SeriesSearchService {
  this: Elastic4sClient
    with SeriesIndexService
    with SearchConverterService
    with SearchService
    with ConverterService
    with Props
    with ErrorHandling =>
  val seriesSearchService: SeriesSearchService

  class SeriesSearchService extends StrictLogging with SearchService[api.SeriesSummaryDTO] {
    import props._

    override val searchIndex: String = SeriesSearchIndex

    override def hitToApiModel(hitString: String, language: String): Try[api.SeriesSummaryDTO] = {
      for {
        searchable <- CirceUtil.tryParseAs[SearchableSeries](hitString)
        result     <- searchConverterService.asSeriesSummary(searchable, language)
      } yield result
    }

    def matchingQuery(settings: SeriesSearchSettings): Try[domain.SearchResult[api.SeriesSummaryDTO]] = {

      val fullSearch = settings.query.emptySomeToNone match {
        case Some(query) =>
          val languageSearch = (field: String, boost: Double) => {
            languageSpecificSearch(
              field,
              settings.language,
              query,
              boost,
              settings.fallback
            )
          }
          boolQuery()
            .must(
              boolQuery()
                .should(
                  languageSearch("titles", 2),
                  languageSearch("descriptions", 1),
                  idsQuery(query)
                )
            )
        case None => boolQuery()
      }

      executeSearch(settings, fullSearch)
    }

    def executeSearch(
        settings: SeriesSearchSettings,
        queryBuilder: BoolQuery
    ): Try[domain.SearchResult[api.SeriesSummaryDTO]] = {

      val (languageFilter, searchLanguage) = settings.language match {
        case None | Some(AllLanguages) => (None, "*")
        case Some(lang) =>
          if (settings.fallback) (None, lang)
          else (Some(existsQuery(s"titles.$lang")), lang)
      }

      val filters        = List(languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.page.getOrElse(1) * numResults
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow"
        )
        Failure(new Helpers.ResultWindowTooLargeException())
      } else {

        val searchToExecute =
          search(searchIndex)
            .size(numResults)
            .from(startAt)
            .trackTotalHits(true)
            .query(filteredSearch)
            .highlighting(highlight("*"))
            .sortBy(getSortDefinition(settings.sort, searchLanguage))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(ElasticSearchScrollKeepAlive)
          } else { searchToExecute }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            getHits(response.result, searchLanguage).map(hits =>
              domain.SearchResult(
                response.result.totalHits,
                Some(settings.page.getOrElse(1)),
                numResults,
                searchLanguage,
                hits,
                response.result.scrollId
              )
            )
          case Failure(ex) => errorHandler(ex)
        }
      }

    }

    protected override def scheduleIndexDocuments(): Unit = {
      val f = Future(seriesIndexService.indexDocuments(None))
      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          )
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }

}
