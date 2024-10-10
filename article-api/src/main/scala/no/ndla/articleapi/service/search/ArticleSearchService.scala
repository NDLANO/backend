/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.Props
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.{ArticleSummaryV2, ErrorHandling}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.service.ConverterService
import no.ndla.common.implicits._
import no.ndla.common.model.domain.Availability
import no.ndla.language.Language
import no.ndla.mapping.License
import no.ndla.search.Elastic4sClient

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait ArticleSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with ArticleIndexService
    with ConverterService
    with Props
    with ErrorHandling =>
  val articleSearchService: ArticleSearchService

  import props._

  class ArticleSearchService extends StrictLogging with SearchService[api.ArticleSummaryV2] {
    private val noCopyright = boolQuery().not(termQuery("license", License.Copyrighted.toString))

    override val searchIndex: String = ArticleSearchIndex

    override def hitToApiModel(hit: String, language: String): api.ArticleSummaryV2 = {
      converterService.hitAsArticleSummaryV2(hit, language)
    }

    def matchingQuery(settings: SearchSettings): Try[SearchResult[ArticleSummaryV2]] = {
      val fullQuery = settings.query.emptySomeToNone match {
        case Some(query) =>
          val language      = if (settings.fallback) "*" else settings.language
          val titleSearch   = simpleStringQuery(query).field(s"title.$language", 3)
          val introSearch   = simpleStringQuery(query).field(s"introduction.$language", 2)
          val metaSearch    = simpleStringQuery(query).field(s"metaDescription.$language", 1)
          val contentSearch = simpleStringQuery(query).field(s"content.$language", 1)
          val tagSearch     = simpleStringQuery(query).field(s"tags.$language", 1)

          boolQuery()
            .must(
              boolQuery()
                .should(
                  titleSearch,
                  introSearch,
                  metaSearch,
                  contentSearch,
                  tagSearch
                )
            )
        case None => boolQuery()
      }

      executeSearch(fullQuery, settings)
    }

    def executeSearch(queryBuilder: BoolQuery, settings: SearchSettings): Try[SearchResult[ArticleSummaryV2]] = {

      val articleTypesFilter =
        if (settings.articleTypes.nonEmpty) Some(constantScoreQuery(termsQuery("articleType", settings.articleTypes)))
        else None

      val availabilityFilter =
        if (settings.availability.isEmpty) Some(termQuery("availability", Availability.everyone.toString))
        else
          Some(boolQuery().should(settings.availability.map(a => termQuery("availability", a.toString))))

      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))

      val licenseFilter = settings.license match {
        case None        => Some(noCopyright)
        case Some("all") => None
        case Some(lic)   => Some(termQuery("license", lic))
      }

      val (languageFilter, searchLanguage) = settings.language match {
        case "" | Language.AllLanguages =>
          (None, "*")
        case lang =>
          if (settings.fallback)
            (None, "*")
          else (Some(existsQuery(s"title.$lang")), lang)
      }

      val grepCodesFilter =
        if (settings.grepCodes.nonEmpty) Some(termsQuery("grepCodes", settings.grepCodes)) else None

      val filters = List(
        licenseFilter,
        idFilter,
        languageFilter,
        articleTypesFilter,
        grepCodesFilter,
        availabilityFilter
      )

      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.pageSize * settings.page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow"
        )
        Failure(ArticleErrorHelpers.ResultWindowTooLargeException())
      } else {

        val searchToExecute = search(searchIndex)
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
            Success(
              SearchResult[ArticleSummaryV2](
                response.result.totalHits,
                Some(settings.page),
                numResults,
                settings.language,
                getHits(response.result, settings.language),
                response.result.scrollId
              )
            )
          case Failure(ex) => errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        articleIndexService.indexDocuments(None)
      }

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
