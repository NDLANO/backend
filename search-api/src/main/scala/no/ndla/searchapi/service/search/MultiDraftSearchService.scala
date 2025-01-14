/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.aggs.responses.{AggResult, AggSerde}
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.{Query, RangeQuery}
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.{Content, Priority}
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.language.Language.AllLanguages
import no.ndla.language.model.Iso639
import no.ndla.search.AggregationBuilder.{buildTermsAggregation, getAggregationsFromResult}
import no.ndla.search.Elastic4sClient
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api.{ErrorHandling, SubjectAggregationDTO, SubjectAggregationsDTO}
import no.ndla.searchapi.model.domain.{LearningResourceType, SearchResult}
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.model.search.settings.MultiDraftSearchSettings

import scala.util.{Failure, Success, Try}

trait MultiDraftSearchService {
  this: Elastic4sClient & SearchConverterService & IndexService & SearchService & DraftIndexService &
    LearningPathIndexService & Props & ErrorHandling & DraftConceptIndexService =>
  val multiDraftSearchService: MultiDraftSearchService

  class MultiDraftSearchService extends StrictLogging with SearchService with TaxonomyFiltering {
    import props.{ElasticSearchScrollKeepAlive, SearchIndex}
    override val searchIndex: List[String] = List(
      SearchType.Drafts,
      SearchType.LearningPaths,
      SearchType.Concepts
    ).map(SearchIndex)

    override val indexServices: List[IndexService[? <: Content]] = List(
      draftIndexService,
      learningPathIndexService,
      draftConceptIndexService
    )

    private case class SumAggResult(value: Long) extends AggResult

    private val sumAggsSerde: AggSerde[SumAggResult] = (_: String, data: Map[String, Any]) => {
      val value = data("value").asInstanceOf[Double].toLong
      SumAggResult(value)
    }

    private def aggregateFavorites(subjectId: String): Try[Long] = {
      val filter       = nestedQuery("contexts", boolQuery().should(termQuery("contexts.rootId", subjectId)))
      val aggregations = sumAgg("favoritedCount", "favorited")
      val searchToExecute = search(searchIndex)
        .query(filter)
        .trackTotalHits(true)
        .size(0)
        .aggs(aggregations)
      e4sClient.execute(searchToExecute).map { res =>
        res.result.aggregations.result("favoritedCount")(sumAggsSerde).value
      }
    }

    def aggregateSubjects(subjects: List[String]): Try[SubjectAggregationsDTO] = {
      val fiveYearsAgo        = NDLADate.now().minusYears(5)
      val inOneYear           = NDLADate.now().plusYears(1)
      val flowExcludeStatuses = List(DraftStatus.ARCHIVED, DraftStatus.PUBLISHED, DraftStatus.UNPUBLISHED)
      val flowStatuses        = DraftStatus.values.filterNot(s => flowExcludeStatuses.contains(s)).toList
      def aggregateSubject(subjectId: String): Try[SubjectAggregationDTO] = for {
        old <- filteredCountSearch(
          MultiDraftSearchSettings.default.copy(
            subjects = List(subjectId),
            publishedFilterTo = Some(fiveYearsAgo)
          )
        )
        revisions <- filteredCountSearch(
          MultiDraftSearchSettings.default.copy(
            subjects = List(subjectId),
            revisionDateFilterTo = Some(inOneYear)
          )
        )
        publishedArticles <- filteredCountSearch(
          MultiDraftSearchSettings.default.copy(
            subjects = List(subjectId),
            statusFilter = List(DraftStatus.PUBLISHED)
          )
        )
        inFlow <- filteredCountSearch(
          MultiDraftSearchSettings.default.copy(
            subjects = List(subjectId),
            statusFilter = flowStatuses
          )
        )
        favorited <- aggregateFavorites(subjectId)
      } yield SubjectAggregationDTO(
        subjectId = subjectId,
        publishedArticleCount = publishedArticles,
        oldArticleCount = old,
        revisionCount = revisions,
        flowCount = inFlow,
        favoritedCount = favorited
      )

      subjects
        .traverse(subjectId => aggregateSubject(subjectId))
        .map(aggregations => SubjectAggregationsDTO(aggregations))
    }

    private def getSearchIndexes(settings: MultiDraftSearchSettings): Try[List[String]] = {
      settings.resultTypes match {
        case Some(list) if list.nonEmpty =>
          val idxs = list.map { st =>
            val index        = SearchIndex(st)
            val isValidIndex = searchIndex.contains(index)

            if (isValidIndex) Right(index)
            else {
              val validSearchTypes = searchIndex.traverse(props.indexToSearchType).getOrElse(List.empty)
              val validTypesString = s"[${validSearchTypes.mkString("'", "','", "'")}]"
              Left(
                ValidationMessage(
                  "resultTypes",
                  s"Invalid result type for endpoint: '$st', expected one of: $validTypesString"
                )
              )
            }
          }

          val errors = idxs.collect { case Left(e) => e }
          if (errors.nonEmpty) Failure(new ValidationException(s"Got invalid `resultTypes` for endpoint", errors))
          else Success(idxs.collect { case Right(i) => i })

        case _ => Success(List(SearchType.Drafts, SearchType.LearningPaths).map(SearchIndex))
      }
    }

    def matchingQuery(settings: MultiDraftSearchSettings): Try[SearchResult] = {

      val contentSearch = settings.query.map(queryString => {

        val langQueryFunc = (fieldName: String, boost: Double) =>
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
            simpleStringQuery(queryString.underlying).field("authors", 1),
            simpleStringQuery(queryString.underlying).field("grepContexts.title", 1),
            nestedQuery("contexts", boolQuery().should(termQuery("contexts.contextId", queryString.underlying)))
              .ignoreUnmapped(true),
            termQuery("contextids", queryString.underlying),
            idsQuery(queryString.underlying),
            nestedQuery("revisionMeta", simpleStringQuery(queryString.underlying).field("revisionMeta.note"))
              .ignoreUnmapped(true)
          ) ++
            getRevisionHistoryLogQuery(queryString.underlying, settings.excludeRevisionHistory) ++
            buildNestedEmbedField(List(queryString.underlying), None, settings.language, settings.fallback) ++
            buildNestedEmbedField(List.empty, Some(queryString.underlying), settings.language, settings.fallback)
        )

      })

      val noteSearch = settings.noteQuery.map(q => {
        boolQuery()
          .should(
            simpleStringQuery(q.underlying).field("notes", 1),
            simpleStringQuery(q.underlying).field("previousVersionsNotes", 1)
          )
      })

      val boolQueries: List[BoolQuery] = List(contentSearch, noteSearch).flatten
      val fullQuery                    = boolQuery().must(boolQueries)

      executeSearch(settings, fullQuery)
    }

    private def filteredCountSearch(settings: MultiDraftSearchSettings): Try[Long] = {
      val filteredSearch = boolQuery().filter(getSearchFilters(settings))
      val aggregations   = buildTermsAggregation(settings.aggregatePaths, indexServices.map(_.getMapping))
      val searchToExecute = search(searchIndex)
        .query(filteredSearch)
        .trackTotalHits(true)
        .from(0)
        .size(0)
        .highlighting(highlight("*"))
        .aggs(aggregations)
      e4sClient.execute(searchToExecute).map(_.result.totalHits)
    }

    def executeSearch(settings: MultiDraftSearchSettings, baseQuery: BoolQuery): Try[SearchResult] = {
      val searchLanguage = settings.language match {
        case lang if Iso639.get(lang).isSuccess && !settings.fallback => lang
        case _                                                        => AllLanguages
      }
      val filteredSearch = baseQuery.filter(getSearchFilters(settings))
      val pagination     = getStartAtAndNumResults(settings.page, settings.pageSize).?
      val aggregations   = buildTermsAggregation(settings.aggregatePaths, indexServices.map(_.getMapping))
      val index          = getSearchIndexes(settings).?
      val searchToExecute = search(index)
        .query(filteredSearch)
        .suggestions(suggestions(settings.query.underlying, searchLanguage, settings.fallback))
        .trackTotalHits(true)
        .from(pagination.startAt)
        .size(pagination.pageSize)
        .highlighting(highlight("*"))
        .aggs(aggregations)
        .sortBy(getSortDefinition(settings.sort, searchLanguage))

      // Only add scroll param if it is first page
      val searchWithScroll =
        if (pagination.startAt == 0 && settings.shouldScroll) {
          searchToExecute.scroll(ElasticSearchScrollKeepAlive)
        } else { searchToExecute }

      e4sClient.execute(searchWithScroll) match {
        case Success(response) =>
          getHits(response.result, settings.language, settings.filterInactive).map(hits => {
            SearchResult(
              totalCount = response.result.totalHits,
              page = Some(settings.page),
              pageSize = pagination.pageSize,
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

    /** Returns a list of QueryDefinitions of different search filters depending on settings.
      *
      * @param settings
      *   SearchSettings object.
      * @return
      *   List of QueryDefinitions.
      */
    private def getSearchFilters(settings: MultiDraftSearchSettings): List[Query] = {
      val languageFilter = settings.language match {
        case "" | AllLanguages      => None
        case _ if settings.fallback => None
        case lang                   => Some(existsQuery(s"title.$lang"))
      }

      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))

      val licenseFilter = settings.license match {
        case Some("all") | None => None
        case Some(lic)          => Some(termQuery("license", lic))
      }

      val grepCodesFilter =
        if (settings.grepCodes.nonEmpty) Some(termsQuery("grepContexts.code", settings.grepCodes))
        else None

      val traitsFilter =
        if (settings.traits.nonEmpty) Some(termsQuery("traits", settings.traits.map(_.entryName)))
        else None

      val embedResourceAndIdFilter =
        buildNestedEmbedField(settings.embedResource, settings.embedId, settings.language, settings.fallback)

      val statusFilter = draftStatusFilter(settings.statusFilter, settings.includeOtherStatuses)
      val usersFilter  = boolUsersFilter(settings.userFilter)
      val revisionDateFilter =
        dateRangeFilter("nextRevision.revisionDate", settings.revisionDateFilterFrom, settings.revisionDateFilterTo)
      val publishedDateFilter = dateRangeFilter("published", settings.publishedFilterFrom, settings.publishedFilterTo)
      val supportedLanguageFilter = supportedLanguagesFilter(settings.supportedLanguages)
      val responsibleIdFilter = Option.when(settings.responsibleIdFilter.nonEmpty) {
        termsQuery("responsible.responsibleId", settings.responsibleIdFilter)
      }

      val prioritizedFilter = settings.prioritized.map { priority =>
        termQuery("priority", if (priority) Priority.Prioritized.entryName else Priority.Unspecified.entryName)
      }

      val priorityFilter = Option.when(settings.priority.nonEmpty)(
        boolQuery().should(settings.priority.map(termQuery("priority", _)))
      )

      val articleTypeFilter = Some(
        boolQuery().should(settings.articleTypes.map(articleType => termQuery("articleType", articleType)))
      )
      val learningResourceType        = learningResourceFilter(settings.learningResourceTypes)
      val taxonomyResourceTypesFilter = resourceTypeFilter(settings.resourceTypes, filterByNoResourceType = false)
      val taxonomySubjectFilter       = subjectFilter(settings.subjects, settings.filterInactive)
      val conceptSubjectFilter        = subjectFilterForConcept(settings.subjects)
      val taxonomyTopicFilter         = topicFilter(settings.topics, settings.filterInactive)
      val taxonomyRelevanceFilter     = relevanceFilter(settings.relevanceIds, settings.subjects)
      val taxonomyContextActiveFilter = contextActiveFilter(settings.filterInactive)

      List(
        licenseFilter,
        idFilter,
        articleTypeFilter,
        languageFilter,
        taxonomySubjectFilter,
        conceptSubjectFilter,
        taxonomyTopicFilter,
        taxonomyResourceTypesFilter,
        taxonomyContextActiveFilter,
        supportedLanguageFilter,
        taxonomyRelevanceFilter,
        statusFilter,
        usersFilter,
        grepCodesFilter,
        traitsFilter,
        embedResourceAndIdFilter,
        revisionDateFilter,
        publishedDateFilter,
        responsibleIdFilter,
        prioritizedFilter,
        priorityFilter,
        learningResourceType
      ).flatten
    }

    private def subjectFilterForConcept(subjectIds: List[String]): Option[Query] = {
      Option.when(subjectIds.nonEmpty) {
        mustBeNotConceptOr(termsQuery("subjectIds", subjectIds))
      }
    }

    private def learningResourceFilter(types: List[LearningResourceType]): Option[Query] =
      Option.when(types.nonEmpty)(
        termsQuery("learningResourceType", types.map(_.entryName))
      )

    private def getRevisionHistoryLogQuery(queryString: String, excludeHistoryLog: Boolean): Seq[Query] = {
      Seq(
        simpleStringQuery(queryString).field("notes", 1)
      ) ++ Option
        .when(!excludeHistoryLog)(
          simpleStringQuery(queryString).field("previousVersionsNotes", 1)
        )
    }

    private def dateToEs(date: NDLADate): Long = date.toUTCEpochSecond * 1000
    private def dateRangeFilter(field: String, from: Option[NDLADate], to: Option[NDLADate]): Option[RangeQuery] = {
      val fromDate = from.map(dateToEs)
      val toDate   = to.map(dateToEs)

      Option.when(fromDate.nonEmpty || toDate.nonEmpty)(
        RangeQuery(
          field = field,
          gte = fromDate,
          lte = toDate
        )
      )
    }

    private def draftStatusFilter(statuses: Seq[DraftStatus], includeOthers: Boolean): Some[BoolQuery] = {
      if (statuses.isEmpty) {
        Some(
          boolQuery().not(termQuery("draftStatus.current", DraftStatus.ARCHIVED.toString))
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
  }

}
