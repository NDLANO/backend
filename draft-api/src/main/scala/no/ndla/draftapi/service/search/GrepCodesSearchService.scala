/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.queries.BoolQuery
import com.sksamuel.elastic4s.searches.sort.SortOrder
import com.typesafe.scalalogging.LazyLogging
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.DraftApiProperties.{ElasticSearchIndexMaxResultWindow, ElasticSearchScrollKeepAlive}
import no.ndla.draftapi.integration.Elastic4sClient
import no.ndla.draftapi.model.api.ResultWindowTooLargeException
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.model.search.SearchableGrepCode
import org.json4s._
import org.json4s.native.Serialization.read

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait GrepCodesSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with GrepCodesIndexService
    with SearchConverterService =>
  val grepCodesSearchService: GrepCodesSearchService

  class GrepCodesSearchService extends LazyLogging with BasicSearchService[String] {
    override val searchIndex: String = DraftApiProperties.DraftGrepCodesSearchIndex
    implicit val formats: Formats = DefaultFormats

    def getHits(response: SearchResponse): Seq[String] = {
      response.hits.hits.toList.map(hit => read[SearchableGrepCode](hit.sourceAsString).grepCode)
    }

    def all(
        language: String,
        page: Int,
        pageSize: Int
    ): Try[LanguagelessSearchResult[String]] = executeSearch(page, pageSize, boolQuery())

    def matchingQuery(query: String, page: Int, pageSize: Int): Try[LanguagelessSearchResult[String]] = {

      val fullQuery = boolQuery()
        .must(
          boolQuery().should(
            matchQuery("grepCode", query.toLowerCase).boost(2),
            prefixQuery("grepCode", query.toLowerCase)
          )
        )

      executeSearch(page, pageSize, fullQuery)
    }

    def executeSearch(
        page: Int,
        pageSize: Int,
        queryBuilder: BoolQuery,
    ): Try[LanguagelessSearchResult[String]] = {
      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(new ResultWindowTooLargeException())
      } else {
        val searchToExecute = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .query(queryBuilder)
          .sortBy(fieldSort("_score").sortOrder(SortOrder.Desc))

        val searchWithScroll =
          if (startAt != 0) { searchToExecute } else { searchToExecute.scroll(ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              LanguagelessSearchResult(
                response.result.totalHits,
                Some(page),
                numResults,
                getHits(response.result),
                response.result.scrollId
              ))
          case Failure(ex) =>
            errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        grepCodesIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of grepCodes of ${reindexResult.totalIndexed} articles in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }
}
