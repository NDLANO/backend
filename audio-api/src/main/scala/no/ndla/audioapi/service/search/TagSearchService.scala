/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.ErrorHandling
import no.ndla.audioapi.model.domain.{SearchResult, SearchableTag}
import no.ndla.common.CirceUtil
import no.ndla.language.model.Iso639
import no.ndla.search.Elastic4sClient

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait TagSearchService {
  this: Elastic4sClient & SearchConverterService & SearchService & TagIndexService & SearchConverterService & Props &
    ErrorHandling =>
  lazy val tagSearchService: TagSearchService

  class TagSearchService extends StrictLogging with SearchService[String] {
    override val searchIndex: String = props.AudioTagSearchIndex

    override def hitToApiModel(hit: String, language: String): Try[String] = {
      CirceUtil.tryParseAs[SearchableTag](hit).map(_.tag)
    }

    def matchingQuery(query: String, searchLanguage: String, page: Int, pageSize: Int): Try[SearchResult[String]] = {
      val language = searchLanguage match {
        case lang if Iso639.get(lang).isSuccess => lang
        case _                                  => "*"
      }

      val fullQuery = boolQuery()
        .must(
          boolQuery().should(
            matchQuery("tag", query).boost(2),
            prefixQuery("tag", query)
          )
        )

      executeSearch(language, page, pageSize, fullQuery)
    }

    def executeSearch(
        language: String,
        page: Int,
        pageSize: Int,
        queryBuilder: BoolQuery
    ): Try[SearchResult[String]] = {

      val languageFilter =
        if (language == "*") None
        else
          Some(
            termQuery("language", language)
          )

      val filters        = List(languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(Some(page), Some(pageSize))
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > props.ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are ${props.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow"
        )
        Failure(new Helpers.ResultWindowTooLargeException())
      } else {
        val searchToExecute = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .trackTotalHits(true)
          .query(filteredSearch)
          .sortBy(fieldSort("_score").sortOrder(SortOrder.Desc))

        val searchWithScroll =
          if (startAt != 0) { searchToExecute }
          else { searchToExecute.scroll(props.ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            getHits(response.result, language).map(hits =>
              SearchResult(
                totalCount = response.result.totalHits,
                page = Some(page),
                pageSize = numResults,
                language = language,
                results = hits,
                scrollId = response.result.scrollId
              )
            )
          case Failure(ex) =>
            errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        tagIndexService.indexDocuments(None)
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of tags of ${reindexResult.totalIndexed} articles in ${reindexResult.millisUsed} ms."
          )
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }
}
