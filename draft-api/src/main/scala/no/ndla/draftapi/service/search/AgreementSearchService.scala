/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import java.util.concurrent.Executors
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.typesafe.scalalogging.StrictLogging
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api.ErrorHelpers
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.service.ConverterService
import no.ndla.language.Language
import no.ndla.search.Elastic4sClient

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait AgreementSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with AgreementIndexService
    with ConverterService
    with Props
    with ErrorHelpers =>
  val agreementSearchService: AgreementSearchService

  class AgreementSearchService extends StrictLogging with SearchService[api.AgreementSummary] {
    import props._
    override val searchIndex: String = props.AgreementSearchIndex

    override def hitToApiModel(hit: String, language: String): api.AgreementSummary = {
      searchConverterService.hitAsAgreementSummary(hit)
    }

    def matchingQuery(settings: AgreementSearchSettings): Try[SearchResult[api.AgreementSummary]] = {

      val fullQuery = settings.query match {
        case Some(query) =>
          boolQuery()
            .must(
              boolQuery()
                .should(
                  queryStringQuery(query).field("title").boost(2),
                  queryStringQuery(query).field("content").boost(1)
                )
            )
        case None => boolQuery()
      }

      executeSearch(settings, fullQuery)
    }

    def executeSearch(
        settings: AgreementSearchSettings,
        queryBuilder: BoolQuery
    ): Try[SearchResult[api.AgreementSummary]] = {
      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))

      val licenseFilter = settings.license match {
        case None      => None
        case Some(lic) => Some(termQuery("license", lic))
      }

      val filters        = List(idFilter, licenseFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.pageSize * settings.page
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
          .sortBy(getSortDefinition(settings.sort))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(ElasticSearchScrollKeepAlive)
          } else { searchToExecute }

        e4sClient.execute { searchWithScroll } match {
          case Success(response) =>
            Success(
              SearchResult(
                response.result.totalHits,
                Some(settings.page),
                numResults,
                Language.NoLanguage,
                getHits(response.result, Language.NoLanguage),
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
        agreementIndexService.indexDocuments(None)
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} agreements in ${reindexResult.millisUsed} ms."
          )
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }
}
