/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import java.util.concurrent.Executors
import com.sksamuel.elastic4s.http.ElasticDsl.{simpleStringQuery, _}
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, Query}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.language.model.Iso639
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.SearchApiProperties.{
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive,
  SearchIndexes
}
import no.ndla.searchapi.integration.Elastic4sClient
import no.ndla.searchapi.model.api.ResultWindowTooLargeException
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.{Language, RequestInfo, SearchResult, draft}
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.model.search.settings.MultiDraftSearchSettings

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

trait MultiDraftSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with IndexService
    with SearchService
    with DraftIndexService
    with LearningPathIndexService =>
  val multiDraftSearchService: MultiDraftSearchService

  class MultiDraftSearchService extends LazyLogging with SearchService with TaxonomyFiltering {
    override val searchIndex = List(SearchIndexes(SearchType.Drafts), SearchIndexes(SearchType.LearningPaths))
    override val indexServices = List(draftIndexService, learningPathIndexService)

    def matchingQuery(settings: MultiDraftSearchSettings): Try[SearchResult] = {

      val contentSearch = settings.query.map(queryString => {

        val langQueryFunc = (fieldName: String, boost: Int) =>
          buildSimpleStringQueryForField(
            queryString,
            fieldName,
            boost,
            settings.language,
            settings.fallback,
            searchDecompounded = settings.searchDecompounded
        )

        boolQuery().should(
          List(
            langQueryFunc("title", 3),
            langQueryFunc("introduction", 2),
            langQueryFunc("metaDescription", 1),
            langQueryFunc("content", 1),
            langQueryFunc("tags", 1),
            langQueryFunc("embedAttributes", 1),
            simpleStringQuery(queryString).field("authors", 1),
            simpleStringQuery(queryString).field("notes", 1),
            simpleStringQuery(queryString).field("previousVersionsNotes", 1),
            simpleStringQuery(queryString).field("grepContexts.title", 1),
            idsQuery(queryString)
          ) ++
            buildNestedEmbedField(List(queryString), None, settings.language, settings.fallback) ++
            buildNestedEmbedField(List.empty, Some(queryString), settings.language, settings.fallback)
        )

      })

      val noteSearch = settings.noteQuery.map(q => {
        boolQuery()
          .should(
            simpleStringQuery(q).field("notes", 1),
            simpleStringQuery(q).field("previousVersionsNotes", 1)
          )
      })

      val boolQueries: List[BoolQuery] = List(contentSearch, noteSearch).flatten
      val fullQuery = boolQuery().must(boolQueries)

      executeSearch(settings, fullQuery)
    }

    def executeSearch(settings: MultiDraftSearchSettings, baseQuery: BoolQuery): Try[SearchResult] = {
      val searchLanguage = settings.language match {
        case lang if Iso639.get(lang).isSuccess && !settings.fallback => lang
        case _                                                        => Language.AllLanguages
      }
      val filteredSearch = baseQuery.filter(getSearchFilters(settings))

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.pageSize * settings.page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(ResultWindowTooLargeException())
      } else {

        val aggregations = buildTermsAggregation(settings.aggregatePaths)

        val searchToExecute = search(searchIndex)
          .query(filteredSearch)
          .suggestions(suggestions(settings.query, searchLanguage, settings.fallback))
          .from(startAt)
          .size(numResults)
          .highlighting(highlight("*"))
          .aggs(aggregations)
          .sortBy(getSortDefinition(settings.sort, searchLanguage))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(ElasticSearchScrollKeepAlive)
          } else { searchToExecute }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult(
                totalCount = response.result.totalHits,
                page = Some(settings.page),
                pageSize = numResults,
                language = searchLanguage,
                results = getHits(response.result, settings.language),
                suggestions = getSuggestions(response.result),
                aggregations = getAggregationsFromResult(response.result),
                scrollId = response.result.scrollId
              ))
          case Failure(ex) => Failure(ex)
        }
      }
    }

    /**
      * Returns a list of QueryDefinitions of different search filters depending on settings.
      *
      * @param settings SearchSettings object.
      * @return List of QueryDefinitions.
      */
    private def getSearchFilters(settings: MultiDraftSearchSettings): List[Query] = {
      val languageFilter = settings.language match {
        case "" | Language.AllLanguages =>
          None
        case lang =>
          if (settings.fallback) None else Some(existsQuery(s"title.$lang"))
      }

      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))

      val licenseFilter = settings.license match {
        case None      => Some(boolQuery().not(termQuery("license", "copyrighted")))
        case Some(lic) => Some(termQuery("license", lic))
      }
      val grepCodesFilter =
        if (settings.grepCodes.nonEmpty) Some(termsQuery("grepContexts.code", settings.grepCodes))
        else None

      val embedResourceAndIdFilter =
        buildNestedEmbedField(settings.embedResource, settings.embedId, settings.language, settings.fallback)

      val statusFilter = draftStatusFilter(settings.statusFilter, settings.includeOtherStatuses)
      val usersFilter = boolUsersFilter(settings.userFilter)

      val taxonomyContextFilter = contextTypeFilter(settings.learningResourceTypes)
      val taxonomyResourceTypesFilter = resourceTypeFilter(settings.resourceTypes, filterByNoResourceType = false)
      val taxonomySubjectFilter = subjectFilter(settings.subjects)
      val taxonomyTopicFilter = topicFilter(settings.topics)
      val taxonomyRelevanceFilter = relevanceFilter(settings.relevanceIds, settings.subjects)

      val supportedLanguageFilter =
        if (settings.supportedLanguages.isEmpty) None
        else
          Some(
            boolQuery().should(
              settings.supportedLanguages.map(l => termQuery("supportedLanguages", l))
            )
          )

      List(
        licenseFilter,
        idFilter,
        languageFilter,
        taxonomySubjectFilter,
        taxonomyTopicFilter,
        taxonomyResourceTypesFilter,
        taxonomyContextFilter,
        supportedLanguageFilter,
        taxonomyRelevanceFilter,
        statusFilter,
        usersFilter,
        grepCodesFilter,
        embedResourceAndIdFilter
      ).flatten
    }

    private def draftStatusFilter(statuses: Seq[draft.ArticleStatus.Value], includeOthers: Boolean): Some[BoolQuery] = {
      if (statuses.isEmpty) {
        Some(
          boolQuery().not(termQuery("draftStatus.current", ArticleStatus.ARCHIVED.toString))
        )
      } else {
        val draftStatuses =
          if (includeOthers) Seq("draftStatus.current", "draftStatus.other")
          else Seq("draftStatus.current")

        Some(
          boolQuery().should(draftStatuses.flatMap(ds => statuses.map(s => termQuery(ds, s.toString))))
        )
      }
    }

    private def boolUsersFilter(users: Seq[String]): Option[BoolQuery] =
      if (users.isEmpty) None
      else
        Some(
          boolQuery().should(users.map(simpleStringQuery(_).field("users", 1)))
        )

    override def scheduleIndexDocuments(): Unit = {
      val threadPoolSize = if (searchIndex.nonEmpty) searchIndex.size else 1
      implicit val ec: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadPoolSize))
      val requestInfo = RequestInfo()

      val draftFuture = Future {
        requestInfo.setRequestInfo()
        draftIndexService.indexDocuments()
      }
      val learningPathFuture = Future {
        requestInfo.setRequestInfo()
        learningPathIndexService.indexDocuments()
      }

      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.Drafts), draftFuture)
      handleScheduledIndexResults(SearchApiProperties.SearchIndexes(SearchType.LearningPaths), learningPathFuture)
    }
  }

}
