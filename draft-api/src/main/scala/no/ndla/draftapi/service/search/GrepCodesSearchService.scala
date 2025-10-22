/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.typesafe.scalalogging.StrictLogging
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.model.domain.LanguagelessSearchResult
import no.ndla.language.Language
import no.ndla.search.NdlaE4sClient
import io.circe.parser.decode
import no.ndla.draftapi.model.api.DraftErrorHelpers
import no.ndla.draftapi.model.search.SearchableGrepCode

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

class GrepCodesSearchService(using
    e4sClient: NdlaE4sClient,
    searchConverterService: SearchConverterService,
    grepCodesIndexService: => GrepCodesIndexService,
    props: DraftApiProperties,
    draftErrorHelpers: DraftErrorHelpers,
) extends SearchService[String]
    with StrictLogging {
  override val searchIndex: String = props.DraftGrepCodesSearchIndex

  override def hitToApiModel(hit: String, language: String): String = {
    decode[SearchableGrepCode](hit) match {
      case Right(searchableGrepCode) => searchableGrepCode.grepCode
      case Left(_)                   => hit // fallback to raw hit if parsing fails
    }
  }

  def matchingQuery(query: String, page: Int, pageSize: Int): Try[LanguagelessSearchResult[String]] = {
    val fullQuery = boolQuery().must(
      boolQuery().should(matchQuery("grepCode", query.toLowerCase).boost(2), prefixQuery("grepCode", query.toLowerCase))
    )

    executeSearch(page, pageSize, fullQuery)
  }

  def executeSearch(page: Int, pageSize: Int, queryBuilder: BoolQuery): Try[LanguagelessSearchResult[String]] = {
    val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
    val requestedResultWindow = pageSize * page
    if (requestedResultWindow > props.ElasticSearchIndexMaxResultWindow) {
      logger.info(
        s"Max supported results are ${props.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow"
      )
      Failure(draftErrorHelpers.ResultWindowTooLargeException())
    } else {
      val searchToExecute = search(searchIndex)
        .size(numResults)
        .from(startAt)
        .trackTotalHits(true)
        .query(queryBuilder)
        .sortBy(fieldSort("_score").sortOrder(SortOrder.Desc))

      val searchWithScroll =
        if (startAt != 0) {
          searchToExecute
        } else {
          searchToExecute.scroll(props.ElasticSearchScrollKeepAlive)
        }

      e4sClient.execute(searchWithScroll) match {
        case Success(response) => Success(
            LanguagelessSearchResult(
              response.result.totalHits,
              Some(page),
              numResults,
              getHits(response.result, Language.AllLanguages),
              response.result.scrollId,
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
      grepCodesIndexService.indexDocuments(None)
    }

    f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
    f.foreach {
      case Success(reindexResult) => logger.info(
          s"Completed indexing of grepCodes of ${reindexResult.totalIndexed} articles in ${reindexResult.millisUsed} ms."
        )
      case Failure(ex) => logger.warn(ex.getMessage, ex)
    }
  }
}
