/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.RequestFailure
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.{NestedQuery, Query}
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.*
import no.ndla.common.model.domain.learningpath.LearningPathStatus
import no.ndla.language.Language.{AllLanguages, NoLanguage}
import no.ndla.language.model.Iso639
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.api.{ErrorHandling, LearningPathSummaryV2DTO}
import no.ndla.learningpathapi.model.domain.*
import no.ndla.learningpathapi.model.search.SearchableLearningPath
import no.ndla.search.{Elastic4sClient, IndexNotFoundException, NdlaSearchException}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}
import no.ndla.learningpathapi.integration.TaxonomyApiClient

trait SearchService extends StrictLogging {
  this: SearchIndexService & Elastic4sClient & SearchConverterServiceComponent & TaxonomyApiClient & Props &
    ErrorHandling =>
  lazy val searchService: SearchService

  class SearchService {
    def scroll(scrollId: String, language: String): Try[SearchResult] =
      e4sClient
        .execute {
          searchScroll(scrollId, props.ElasticSearchScrollKeepAlive)
        }
        .map(response => {
          val hits = getHitsV2(response.result, language)

          SearchResult(
            totalCount = response.result.totalHits,
            page = None,
            pageSize = response.result.hits.hits.length,
            language = language,
            results = hits,
            scrollId = response.result.scrollId
          )
        })

    private def getHitsV2(response: SearchResponse, language: String): Seq[LearningPathSummaryV2DTO] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

          resultArray.map(result => {
            val matchedLanguage = language match {
              case AllLanguages =>
                searchConverterService
                  .getLanguageFromHit(result)
                  .getOrElse(language)
              case _ => language
            }

            hitAsLearningPathSummaryV2(result.sourceAsString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    private def hitAsLearningPathSummaryV2(hitString: String, language: String): LearningPathSummaryV2DTO = {
      val searchable = CirceUtil.unsafeParseAs[SearchableLearningPath](hitString)
      searchConverterService.asApiLearningPathSummaryV2(searchable, language)
    }

    def containsArticle(id: Long): Try[Seq[LearningPathSummaryV2DTO]] = {
      val nodes      = taxonomyApiClient.queryNodes(id).getOrElse(List.empty).flatMap(_.paths)
      val plainPaths = List(
        s"/article-iframe/*/$id",
        s"/article-iframe/*/$id/",
        s"/article-iframe/*/$id/\\?*",
        s"/article-iframe/*/$id\\?*",
        s"/article/$id"
      )
      val paths = nodes ++ plainPaths

      val settings = SearchSettings(
        query = None,
        withIdIn = List.empty,
        withPaths = paths,
        taggedWith = None,
        language = Some(AllLanguages),
        sort = Sort.ByTitleAsc,
        page = None,
        pageSize = None,
        fallback = false,
        articleId = Some(id),
        verificationStatus = None,
        shouldScroll = false,
        status = List(
          LearningPathStatus.PUBLISHED,
          LearningPathStatus.SUBMITTED,
          LearningPathStatus.UNLISTED
        )
      )

      executeSearch(boolQuery(), settings).map(_.results)
    }

    private def languageSpecificSearch(searchField: String, language: String, query: String, boost: Double): Query =
      simpleStringQuery(query).field(s"$searchField.$language", boost)

    def matchingQuery(settings: SearchSettings): Try[SearchResult] = {
      val searchLanguage = settings.language match {
        case Some(lang) if Iso639.get(lang).isSuccess => lang
        case _                                        => AllLanguages
      }

      val fullQuery = settings.query.emptySomeToNone match {
        case Some(query) =>
          val language =
            if (settings.fallback) "*" else searchLanguage
          val titleSearch     = languageSpecificSearch("titles", language, query, 2)
          val descSearch      = languageSpecificSearch("descriptions", language, query, 2)
          val stepTitleSearch = languageSpecificSearch("titles", language, query, 1)
          val stepDescSearch  = languageSpecificSearch("descriptions", language, query, 1)
          val tagSearch       = languageSpecificSearch("tags", language, query, 2)
          val authorSearch    = simpleStringQuery(query).field("author", 1)
          boolQuery()
            .must(
              boolQuery()
                .should(
                  titleSearch,
                  descSearch,
                  nestedQuery("learningsteps", stepTitleSearch),
                  nestedQuery("learningsteps", stepDescSearch),
                  tagSearch,
                  authorSearch
                )
            )
        case None if searchLanguage == "*" => boolQuery()
        case _                             =>
          val titleSearch = existsQuery(s"titles.$searchLanguage")
          val descSearch  = existsQuery(s"descriptions.$searchLanguage")
          boolQuery()
            .should(
              titleSearch,
              descSearch
            )
      }

      executeSearch(fullQuery, settings)
    }

    private def getStatusFilter(settings: SearchSettings) = settings.status match {
      case Nil      => Some(termQuery("status", LearningPathStatus.PUBLISHED.entryName))
      case statuses => Some(termsQuery("status", statuses.map(_.entryName)))
    }

    private def executeSearch(queryBuilder: BoolQuery, settings: SearchSettings): Try[SearchResult] = {
      val (languageFilter, searchLanguage) = settings.language match {
        case Some(lang) if settings.fallback => (None, lang)
        case Some(lang)                      => (Some(existsQuery(s"titles.$lang")), lang)
        case _                               => (None, "*")
      }

      val tagFilter: Option[Query] = settings.taggedWith.map(tag => termQuery(s"tags.$searchLanguage.raw", tag))
      val idFilter                 = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))
      val articlesFilter           = articlesFilterQuery(settings.withPaths, settings.articleId)

      val verificationStatusFilter = settings.verificationStatus.map(status => termQuery("verificationStatus", status))

      val statusFilter = getStatusFilter(settings)

      val filters = List(
        tagFilter,
        idFilter,
        articlesFilter,
        languageFilter,
        verificationStatusFilter,
        statusFilter
      )

      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.page.getOrElse(1) * numResults
      if (requestedResultWindow > props.ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are ${props.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow"
        )
        Failure(LearningpathHelpers.ResultWindowTooLargeException())
      } else {
        val searchToExecute = search(props.SearchIndex)
          .size(numResults)
          .from(startAt)
          .trackTotalHits(true)
          .query(filteredSearch)
          .highlighting(highlight("*"))
          .sortBy(getSortDefinition(settings.sort, searchLanguage))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(props.ElasticSearchScrollKeepAlive)
          } else { searchToExecute.explain(true) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult(
                response.result.totalHits,
                Some(settings.page.getOrElse(1)),
                numResults,
                searchLanguage,
                getHitsV2(response.result, searchLanguage),
                response.result.scrollId
              )
            )
          case Failure(ex) =>
            errorHandler(ex)
        }

      }
    }

    private def articlesFilterQuery(paths: List[String], id: Option[Long]): Option[NestedQuery] = {
      if (paths.isEmpty) None
      else {
        val nestedPathsQuery = paths.map(p => wildcardQuery("learningsteps.embedUrl", s"*$p"))
        val articleQuery     = id.map(id => termQuery("learningsteps.articleId", id)).toSeq
        Some(
          nestedQuery(
            "learningsteps",
            boolQuery()
              .should(nestedPathsQuery ++ articleQuery)
              .must(matchQuery("learningsteps.status", "ACTIVE"))
              .minimumShouldMatch(1)
          )
        )
      }
    }

    def countDocuments(): Long = {
      val response = e4sClient.execute {
        catCount(props.SearchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_)    => 0
      }
    }

    private def getSortDefinition(sort: Sort, language: String) = {
      val sortLanguage = language match {
        case NoLanguage => props.DefaultLanguage
        case _          => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          language match {
            case AllLanguages =>
              fieldSort("defaultTitle").order(SortOrder.Asc).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw")
                .order(SortOrder.Asc)
                .missing("_last")
                .unmappedType("long")
          }
        case Sort.ByTitleDesc =>
          language match {
            case AllLanguages =>
              fieldSort("defaultTitle").order(SortOrder.Desc).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw")
                .order(SortOrder.Desc)
                .missing("_last")
                .unmappedType("long")
          }
        case Sort.ByDurationAsc =>
          fieldSort("duration").order(SortOrder.Asc).missing("_last")
        case Sort.ByDurationDesc =>
          fieldSort("duration").order(SortOrder.Desc).missing("_last")
        case Sort.ByLastUpdatedAsc =>
          fieldSort("lastUpdated").order(SortOrder.Asc).missing("_last")
        case Sort.ByLastUpdatedDesc =>
          fieldSort("lastUpdated").order(SortOrder.Desc).missing("_last")
        case Sort.ByRelevanceAsc  => fieldSort("_score").order(SortOrder.Asc)
        case Sort.ByRelevanceDesc => fieldSort("_score").order(SortOrder.Desc)
        case Sort.ByIdAsc         =>
          fieldSort("id").order(SortOrder.Asc).missing("_last")
        case Sort.ByIdDesc =>
          fieldSort("id").order(SortOrder.Desc).missing("_last")
      }
    }

    def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(props.MaxPageSize)
          else props.DefaultPageSize
        case None => props.DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None     => 0
      }

      (startAt, numResults)
    }

    private def errorHandler[T](exception: Throwable): Failure[T] = {
      exception match {
        case NdlaSearchException(_, Some(RequestFailure(status, _, _, _)), _, _) if status == 404 =>
          logger.error(s"Index ${props.SearchIndex} not found. Scheduling a reindex.")
          scheduleIndexDocuments()
          Failure(
            IndexNotFoundException(s"Index ${props.SearchIndex} not found. Scheduling a reindex")
          )
        case e: NdlaSearchException[?] =>
          logger.error(e.getMessage)
          Failure(
            NdlaSearchException(
              s"Unable to execute search in ${props.SearchIndex}: ${e.getMessage}",
              e
            )
          )
        case t => Failure(t)
      }
    }

    private def scheduleIndexDocuments(): Unit = {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        searchIndexService.indexDocuments
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
