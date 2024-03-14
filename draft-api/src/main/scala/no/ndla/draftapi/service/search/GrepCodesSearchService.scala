/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api.ErrorHelpers
import no.ndla.draftapi.model.domain.*
import no.ndla.draftapi.model.search.SearchableGrepCode
import no.ndla.search.Elastic4sClient

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait GrepCodesSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with GrepCodesIndexService
    with SearchConverterService
    with Props
    with ErrorHelpers =>
  val grepCodesSearchService: GrepCodesSearchService

  class GrepCodesSearchService extends StrictLogging with BasicSearchService[String] {
    import props._
    override val searchIndex: String = DraftGrepCodesSearchIndex

    def getHits(response: SearchResponse): Seq[String] = {
      response.hits.hits.toList.map(hit => CirceUtil.unsafeParseAs[SearchableGrepCode](hit.sourceAsString).grepCode)
    }

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
        queryBuilder: BoolQuery
    ): Try[LanguagelessSearchResult[String]] = {
      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow"
        )
        Failure(new ResultWindowTooLargeException())
      } else {
        val searchToExecute = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .trackTotalHits(true)
          .query(queryBuilder)
          .sortBy(fieldSort("_score").sortOrder(SortOrder.Desc))

        val searchWithScroll =
          if (startAt != 0) { searchToExecute }
          else { searchToExecute.scroll(ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              LanguagelessSearchResult(
                response.result.totalHits,
                Some(page),
                numResults,
                getHits(response.result),
                response.result.scrollId
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
        grepCodesIndexService.indexDocuments(None)
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of grepCodes of ${reindexResult.totalIndexed} articles in ${reindexResult.millisUsed} ms."
          )
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }
}
