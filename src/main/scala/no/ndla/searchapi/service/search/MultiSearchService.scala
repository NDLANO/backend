/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */


package no.ndla.searchapi.service.search

import java.util.concurrent.Executors

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.api.{MultiSearchSummary, ResultWindowTooLargeException, SearchResult}
import no.ndla.searchapi.model.domain.{Language, Sort}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait MultiSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with ArticleIndexService =>
  val multiSearchService: MultiSearchService

  class MultiSearchService extends LazyLogging with SearchService[MultiSearchSummary] {
    private val noCopyright = boolQuery().not(termQuery("license", "copyrighted"))

    override val searchIndex: String = SearchApiProperties.SearchIndexes("articles")

    override def hitToApiModel(hit: String, language: String): MultiSearchSummary = {
      searchConverterService.hitAsMultiSummary(hit, language)
    }

    def all(language: String,
            page: Int,
            pageSize: Int,
            sort: Sort.Value,
            types: Seq[String],
            fallback: Boolean): Try[SearchResult[MultiSearchSummary]] = { // TODO: SearchResult with totalLearningpathCount, totalTopicArticleCount... etc?
      executeSearch(language, sort, page, pageSize, boolQuery(), types, fallback)
    }

    def matchingQuery(query: String,
                      searchLanguage: String,
                      page: Int,
                      pageSize: Int,
                      sort: Sort.Value,
                      types: Seq[String],
                      fallback: Boolean): Try[SearchResult[MultiSearchSummary]] = {
      val language = if (searchLanguage == Language.AllLanguages || fallback) "*" else searchLanguage
      val titleSearch = simpleStringQuery(query).field(s"title.$language", 2)
      val introSearch = simpleStringQuery(query).field(s"introduction.$language", 2)
      val metaSearch = simpleStringQuery(query).field(s"metaDescription.$language", 1)
      val contentSearch = simpleStringQuery(query).field(s"content.$language", 1)
      val tagSearch = simpleStringQuery(query).field(s"tags.$language", 1)

      val fullQuery = boolQuery()
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

      executeSearch(searchLanguage, sort, page, pageSize, fullQuery, types, fallback)
    }

    def executeSearch(language: String,
                      sort: Sort.Value,
                      page: Int,
                      pageSize: Int,
                      queryBuilder: BoolQueryDefinition,
                      types: Seq[String],
                      fallback: Boolean): Try[api.SearchResult[MultiSearchSummary]] = {

      val typesFilter = if (types.nonEmpty) Some(constantScoreQuery(termsQuery("type", types))) else None // TODO: update this after type is stored in index somehow

      val (languageFilter, searchLanguage) = language match {
        case "" | Language.AllLanguages =>
          (None, "*")
        case lang =>
          fallback match {
            case true => (None, "*")
            case false => (Some(existsQuery(s"title.$lang")), lang)
          }
      }

      val filters = List(languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > SearchApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${SearchApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else {

        val searchToExec = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .query(filteredSearch)
          .highlighting(highlight("*"))
          .sortBy(getSortDefinition(sort, searchLanguage))

        e4sClient.execute(searchToExec) match {
          case Success(response) =>
            Success(api.SearchResult[MultiSearchSummary](
              response.result.totalHits,
              page,
              numResults,
              if (language == "*") Language.AllLanguages else language,
              getHits(response.result, language, fallback)
            ))
          case Failure(ex) => errorHandler(ex)
        }
      }
    }

    override def scheduleIndexDocuments(): Unit = {
      implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        articleIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }

}
