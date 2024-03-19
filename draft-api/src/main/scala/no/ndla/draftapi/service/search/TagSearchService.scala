/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import java.util.concurrent.Executors
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api.ErrorHelpers
import no.ndla.draftapi.model.domain.*
import no.ndla.draftapi.model.search.SearchableTag
import no.ndla.language.Language
import no.ndla.search.Elastic4sClient

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait TagSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with TagIndexService
    with SearchConverterService
    with Props
    with ErrorHelpers =>
  val tagSearchService: TagSearchService

  class TagSearchService extends StrictLogging with SearchService[String] {
    import props._
    override val searchIndex: String = DraftTagSearchIndex

    override def hitToApiModel(hit: String, language: String): String = {
      val searchableTag = CirceUtil.unsafeParseAs[SearchableTag](hit)
      searchableTag.tag
    }

    def all(
        language: String,
        page: Int,
        pageSize: Int
    ): Try[SearchResult[String]] = executeSearch(language, page, pageSize, boolQuery())

    def matchingQuery(query: String, searchLanguage: String, page: Int, pageSize: Int): Try[SearchResult[String]] = {

      val language = if (searchLanguage == Language.AllLanguages) "*" else searchLanguage

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
          .query(filteredSearch)
          .sortBy(fieldSort("_score").sortOrder(SortOrder.Desc))

        val searchWithScroll =
          if (startAt != 0) { searchToExecute }
          else { searchToExecute.scroll(ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult(
                response.result.totalHits,
                Some(page),
                numResults,
                if (language == "*") Language.AllLanguages else language,
                getHits(response.result, language),
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
