/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.Availability
import no.ndla.language.Language.AllLanguages
import no.ndla.language.model.Iso639
import no.ndla.network.model.RequestInfo
import no.ndla.search.AggregationBuilder.{buildTermsAggregation, getAggregationsFromResult}
import no.ndla.search.Elastic4sClient
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api.ErrorHelpers
import no.ndla.searchapi.model.domain.SearchResult
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.model.search.settings.SearchSettings

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}
import no.ndla.common.model.domain.Content

trait MultiSearchService {
  this: Elastic4sClient & SearchConverterService & SearchService & IndexService & ArticleIndexService &
    LearningPathIndexService & Props & ErrorHelpers =>

  val multiSearchService: MultiSearchService

  class MultiSearchService extends StrictLogging with SearchService with TaxonomyFiltering {
    import props.{ElasticSearchIndexMaxResultWindow, ElasticSearchScrollKeepAlive, SearchIndex}

    override val searchIndex: List[String] = List(SearchType.Articles, SearchType.LearningPaths).map(SearchIndex)
    override val indexServices: List[IndexService[? <: Content]] = List(articleIndexService, learningPathIndexService)

    def matchingQuery(settings: SearchSettings): Try[SearchResult] = {

      val contentSearch = settings.query.map(q => {
        val langQueryFunc = (fieldName: String, boost: Double) =>
          buildSimpleStringQueryForField(
            q,
            fieldName,
            boost,
            settings.language,
            settings.fallback,
            searchDecompounded = true
          )
        boolQuery().must(
          boolQuery().should(
            List(
              langQueryFunc("title", 6),
              langQueryFunc("introduction", 2),
              langQueryFunc("metaDescription", 1),
              langQueryFunc("content", 1),
              langQueryFunc("tags", 1),
              langQueryFunc("embedAttributes", 1),
              simpleStringQuery(q.underlying).field("authors", 1),
              simpleStringQuery(q.underlying).field("grepContexts.title", 1),
              nestedQuery("contexts", boolQuery().should(termQuery("contexts.contextId", q.underlying))),
              idsQuery(q.underlying)
            ) ++
              buildNestedEmbedField(List(q.underlying), None, settings.language, settings.fallback) ++
              buildNestedEmbedField(List.empty, Some(q.underlying), settings.language, settings.fallback)
          )
        )
      })

      val boolQueries: List[BoolQuery] = List(contentSearch).flatten
      val fullQuery                    = boolQuery().must(boolQueries)

      executeSearch(settings, fullQuery)
    }

    def executeSearch(settings: SearchSettings, baseQuery: BoolQuery): Try[SearchResult] = {
      val searchLanguage = settings.language match {
        case lang if Iso639.get(lang).isSuccess && !settings.fallback => lang
        case _                                                        => AllLanguages
      }

      val filteredSearch = baseQuery.filter(getSearchFilters(settings))

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.pageSize * settings.page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow"
        )
        Failure(ResultWindowTooLargeException())
      } else {

        val aggregations = buildTermsAggregation(settings.aggregatePaths, indexServices.map(_.getMapping))

        val searchToExecute = search(searchIndex)
          .query(filteredSearch)
          .suggestions(suggestions(settings.query.underlying, searchLanguage, settings.fallback))
          .from(startAt)
          .trackTotalHits(true)
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
            getHits(response.result, settings.language, settings.filterInactive).map(hits => {
              SearchResult(
                totalCount = response.result.totalHits,
                page = Some(settings.page),
                pageSize = numResults,
                language = searchLanguage,
                results = hits,
                suggestions = getSuggestions(response.result),
                aggregations = getAggregationsFromResult(response.result),
                scrollId = response.result.scrollId
              )
            })
          case Failure(ex) => Failure(ex)
        }
      }
    }

    /** Returns a list of QueryDefinitions of different search filters depending on settings.
      *
      * @param settings
      *   SearchSettings object.
      * @return
      *   List of QueryDefinitions.
      */
    private def getSearchFilters(settings: SearchSettings): List[Query] = {
      val languageFilter = settings.language match {
        case lang if Iso639.get(lang).isSuccess && !settings.fallback =>
          if (settings.fallback) None else Some(existsQuery(s"title.$lang"))
        case _ => None
      }

      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))

      val licenseFilter = settings.license match {
        case None      => Some(boolQuery().not(termQuery("license", "copyrighted")))
        case Some(lic) => Some(termQuery("license", lic))
      }

      val grepCodesFilter =
        if (settings.grepCodes.nonEmpty)
          Some(termsQuery("grepContexts.code", settings.grepCodes))
        else None

      val embedResourceAndIdFilter =
        buildNestedEmbedField(settings.embedResource, settings.embedId, settings.language, settings.fallback)

      val articleTypeFilter = Some(
        boolQuery().should(settings.articleTypes.map(articleType => termQuery("articleType", articleType)))
      )
      val learningResourceTypeFilter = Option.when(settings.learningResourceTypes.nonEmpty)(
        boolQuery().should(
          settings.learningResourceTypes.map(resourceType => termQuery("learningResourceType", resourceType.entryName))
        )
      )
      val taxonomyResourceTypesFilter = resourceTypeFilter(settings.resourceTypes, settings.filterByNoResourceType)
      val taxonomySubjectFilter       = subjectFilter(settings.subjects, settings.filterInactive)
      val taxonomyRelevanceFilter     = relevanceFilter(settings.relevanceIds, settings.subjects)
      val taxonomyContextActiveFilter = contextActiveFilter(settings.filterInactive)

      val supportedLanguageFilter = supportedLanguagesFilter(settings.supportedLanguages)

      val availsToFilterOut = Availability.values.toSet -- (settings.availability.toSet + Availability.everyone)
      val availabilityFilter = Some(
        not(availsToFilterOut.toSeq.map(a => termQuery("availability", a.toString)))
      )

      List(
        licenseFilter,
        idFilter,
        articleTypeFilter,
        learningResourceTypeFilter,
        languageFilter,
        taxonomySubjectFilter,
        taxonomyResourceTypesFilter,
        supportedLanguageFilter,
        taxonomyRelevanceFilter,
        taxonomyContextActiveFilter,
        grepCodesFilter,
        embedResourceAndIdFilter,
        availabilityFilter
      ).flatten
    }

    override def scheduleIndexDocuments(): Unit = {
      val threadPoolSize = if (searchIndex.nonEmpty) searchIndex.size else 1
      implicit val ec: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadPoolSize))
      val requestInfo = RequestInfo.fromThreadContext()

      val articleFuture = Future {
        requestInfo.setThreadContextRequestInfo()
        articleIndexService.indexDocuments(shouldUsePublishedTax = true)
      }
      val learningPathFuture = Future {
        requestInfo.setThreadContextRequestInfo()
        learningPathIndexService.indexDocuments(shouldUsePublishedTax = true)
      }

      handleScheduledIndexResults(SearchIndex(SearchType.Articles), articleFuture)
      handleScheduledIndexResults(SearchIndex(SearchType.LearningPaths), learningPathFuture)
    }
  }

}
