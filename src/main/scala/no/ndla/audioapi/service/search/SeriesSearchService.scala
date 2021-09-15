/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import cats.implicits._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, Query}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties.{
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive,
  SeriesSearchIndex
}
import no.ndla.audioapi.integration.Elastic4sClient
import no.ndla.audioapi.model.Language._
import no.ndla.audioapi.model.api.ResultWindowTooLargeException
import no.ndla.audioapi.model.domain.SeriesSearchSettings
import no.ndla.audioapi.model.search.{SearchableLanguageFormats, SearchableSeries}
import no.ndla.audioapi.model.{Language, api, domain}
import no.ndla.audioapi.service.ConverterService
import org.json4s._
import org.json4s.native.Serialization

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait SeriesSearchService {
  this: Elastic4sClient with SeriesIndexService with SearchConverterService with SearchService with ConverterService =>
  val seriesSearchService: SeriesSearchService

  class SeriesSearchService extends LazyLogging with SearchService[api.SeriesSummary] {

    override val searchIndex: String = SeriesSearchIndex

    override def hitToApiModel(hitString: String, language: String): Try[api.SeriesSummary] = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats ++ org.json4s.ext.JodaTimeSerializers.all
      val searchable = Serialization.read[SearchableSeries](hitString)
      searchConverterService.asSeriesSummary(searchable, language)
    }

    def matchingQuery(settings: SeriesSearchSettings): Try[domain.SearchResult[api.SeriesSummary]] = {

      val fullSearch = settings.query match {
        case Some(query) =>
          boolQuery()
            .must(
              boolQuery()
                .should(
                  languageSpecificSearch("titles", settings.language, query, 2),
                  languageSpecificSearch("descriptions", settings.language, query, 1),
                  idsQuery(query)
                )
            )
        case None => boolQuery()
      }

      executeSearch(settings, fullSearch)
    }

    def executeSearch(settings: SeriesSearchSettings,
                      queryBuilder: BoolQuery): Try[domain.SearchResult[api.SeriesSummary]] = {

      val (languageFilter, searchLanguage) = settings.language match {
        case None | Some(Language.AllLanguages) => (None, "*")
        case Some(lang)                         => (Some(existsQuery(s"titles.$lang")), lang)
      }

      val filters = List(languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.page.getOrElse(1) * numResults
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(new ResultWindowTooLargeException())
      } else {

        val searchToExecute =
          search(searchIndex)
            .size(numResults)
            .from(startAt)
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
            getHits(response.result, searchLanguage).map(
              hits =>
                domain.SearchResult(
                  response.result.totalHits,
                  Some(settings.page.getOrElse(1)),
                  numResults,
                  if (searchLanguage == "*") Language.AllLanguages else searchLanguage,
                  hits,
                  response.result.scrollId
              ))
          case Failure(ex) => errorHandler(ex)
        }
      }

    }

    protected override def scheduleIndexDocuments(): Unit = {
      val f = Future(seriesIndexService.indexDocuments)
      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }

}
